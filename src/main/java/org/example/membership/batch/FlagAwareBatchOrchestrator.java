package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.common.util.ShardUtil;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.StepExecutionLog;
import org.example.membership.entity.User;
import org.example.membership.entity.WasInstance;
import org.example.membership.repository.jpa.StepExecutionLogRepository;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.example.membership.service.jpa.JpaBadgeService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.jpa.JpaOrderService;
import org.mybatis.logging.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlagAwareBatchOrchestrator {


    private final JpaOrderService jpaOrderService;
    private final JpaBadgeService jpaBadgeService;
    private final JpaMembershipService jpaMembershipService;
    private final FlagManager flagManager;

    private final BadgeBatchExecutor badgeBatchExecutor;
    private final UserLevelBatchExecutor userLevelBatchExecutor;
    private final CouponBatchExecutor couponBatchExecutor;

    private final StepExecutionLogRepository stepLogRepository;
    private final WasInstanceRepository wasInstanceRepository;

    private final MyWasInstanceHolder myWasInstanceHolder;




    public void runFullBatch(String targetDate, int batchSize) {
        String myUuid = myWasInstanceHolder.getMyUuid().toString();

        WasInstance self = wasInstanceRepository.findById(myUuid)
                .orElseThrow(() -> new IllegalStateException("WAS 인스턴스 정보 없음"));

        int index = self.getIndex();
        int total = (int) wasInstanceRepository.countRunningInstances();

        runFullBatch(targetDate, batchSize, total, index); // 내부 호출
    }

    public void runFullBatch(String targetDate, int batchSize, int total, int index) {
        if (flagManager.isBadgeBatchRunning()) {
            log.warn("[전체 배치 중복 실행 차단] 이미 전역 배지 배치가 진행 중입니다.");
            return;
        }
        flagManager.startGlobalBadgeBatch();

        Instant allStart = Instant.now();
        long time1, time2, time3;

        List<User> users;
        Map<Long, Map<Long, OrderCountAndAmount>> statMap;
        List<String> keysToFlag;

        try {
            LocalDate date = LocalDate.parse(targetDate);

            // StepExecutionLog 조회 또는 생성
            StepExecutionLog stepLog = stepLogRepository
                    .findByTargetDateAndWasIndex(date, index)
                    .orElseGet(() -> {
                        StepExecutionLog log = new StepExecutionLog();
                        log.setTargetDate(date);
                        log.setWasIndex(index);
                        return stepLogRepository.save(log);
                    });

            // [1] 통계 & 유저 조회
            Instant t1 = Instant.now();
            statMap = jpaOrderService.aggregateUserCategoryStats(date);
            time1 = Duration.between(t1, Instant.now()).toMillis();

            Instant t2 = Instant.now();
            users = jpaMembershipService.getAllUsers();
            time2 = Duration.between(t2, Instant.now()).toMillis();

            // 분기 필터링
            ShardUtil shardUtil = new ShardUtil(total, index);
            users = users.stream()
                    .filter(user -> shardUtil.isMine(user.getId()))
                    .toList();
            log.info("[분기 필터링 완료] index: {}, 유저 수: {}", index, users.size());

            Instant t3 = Instant.now();
            keysToFlag = jpaBadgeService.detectBadgeUpdateTargets(users, statMap);
            time3 = Duration.between(t3, Instant.now()).toMillis();

            // [2] Step 1: 배지 갱신
            if (!stepLog.isBadgeDone()) {
                log.info("[Step1] 배지 갱신 시작");
                badgeBatchExecutor.execute(keysToFlag, batchSize);
                stepLog.setBadgeDone(true);
                stepLogRepository.save(stepLog);
            }

            // [2.5] 스케일아웃 감지
            long currentWasCount = wasInstanceRepository.countRunningInstances();
            if (currentWasCount != total) {
                log.warn("[스케일아웃 감지] total={}, 현재 WAS 수={}. 배치 중단", total, currentWasCount);
                return;
            }

            // [3] Step 2: 등급 갱신
            if (!stepLog.isLevelDone()) {
                log.info("[Step2] 멤버십 등급 갱신 시작");
                userLevelBatchExecutor.execute(users, batchSize);
                stepLog.setLevelDone(true);
                stepLogRepository.save(stepLog);
            }

            // [3.5] 스케일아웃 감지
            currentWasCount = wasInstanceRepository.countRunningInstances();
            if (currentWasCount != total) {
                log.warn("[스케일아웃 감지] total={}, 현재 WAS 수={}. 배치 중단", total, currentWasCount);
                return;
            }

            // [4] Step 3: 쿠폰 발급
            if (!stepLog.isCouponDone()) {
                log.info("[Step3] 쿠폰 발급 시작");
                couponBatchExecutor.execute(users, batchSize);
                stepLog.setCouponDone(true);
                stepLogRepository.save(stepLog);
            }

        } catch (Exception e) {
            flagManager.endGlobalBadgeBatch();
            throw new RuntimeException(e);
        }

        flagManager.endGlobalBadgeBatch();

        long totalTime = Duration.between(allStart, Instant.now()).toMillis();
        log.info("[전체 배치 완료] 총 소요 시간: {}ms", totalTime);
        log.info(" ├─ [1] 통계 집계: {}ms", time1);
        log.info(" ├─ [2] 유저 조회: {}ms", time2);
        log.info(" ├─ [3] 배지 대상 추출: {}ms", time3);
    }
}