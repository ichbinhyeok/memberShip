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
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlagAwareBatchOrchestrator {

    private static final int MAX_RETRIES = 3;
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 30;

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
        UUID myUuid = myWasInstanceHolder.getMyUuid();

        WasInstance self = wasInstanceRepository.findById(myUuid)
                .orElseThrow(() -> new IllegalStateException("WAS 인스턴스 정보 없음"));

        int index = self.getIndex();
        int total = (int) getAliveWasCount();

        runFullBatch(targetDate, batchSize, total, index, 0);
    }

    public void runFullBatch(String targetDate, int batchSize, int total, int index, int retryCount) {
        if (retryCount > MAX_RETRIES) {
            log.error("[배치 중단] 최대 재시도 초과. 반복 방지를 위해 종료합니다.");
            return;
        }

        if (retryCount > 0) {
            flagManager.clearAllFlags();
            log.info("[재시작 초기화] 캐시 및 플래그 상태 초기화 완료 (retry={})", retryCount);
        }

        if (flagManager.isBadgeBatchRunning()) {
            log.warn("[전체 배치 중복 실행 차단] 이미 전역 배지 배치가 진행 중입니다.");
            return;
        }

        flagManager.startGlobalBadgeBatch();
        log.info("[배치 시작] targetDate={}, totalWAS={}, index={}, batchSize={}, retry={}", targetDate, total, index, batchSize, retryCount);

        Instant allStart = Instant.now();
        long time1 = 0, time2 = 0, time3 = 0;
        long badgeTime = 0, levelTime = 0, couponTime = 0;

        List<User> users = null;
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = null;
        List<String> keysToFlag = null;

        try {
            LocalDate date = LocalDate.parse(targetDate);

            StepExecutionLog stepLog = stepLogRepository
                    .findByTargetDateAndWasIndex(date, index)
                    .orElseGet(() -> {
                        log.info("[StepLog 생성] targetDate={}, index={}", date, index);
                        StepExecutionLog log = new StepExecutionLog();
                        log.setTargetDate(date);
                        log.setWasIndex(index);
                        return stepLogRepository.save(log);
                    });

            Instant t1 = Instant.now();
            statMap = jpaOrderService.aggregateUserCategoryStats(date);
            time1 = Duration.between(t1, Instant.now()).toMillis();
            log.info(" ├─ [1] 통계 집계 완료: {}ms", time1);

            Instant t2 = Instant.now();
            users = jpaMembershipService.getAllUsers();
            time2 = Duration.between(t2, Instant.now()).toMillis();
            log.info(" ├─ [2] 유저 조회 완료: {}명 ({}ms)", users.size(), time2);

            int beforeFilter = users.size();
            ShardUtil shardUtil = new ShardUtil(total, index);
            users = users.stream().filter(user -> shardUtil.isMine(user.getId())).toList();
            log.info(" ├─ [3] 분기 필터링 완료: {} → {}명 (index={})", beforeFilter, users.size(), index);

            Instant t3 = Instant.now();
            keysToFlag = jpaBadgeService.detectBadgeUpdateTargets(users, statMap);
            time3 = Duration.between(t3, Instant.now()).toMillis();
            log.info(" ├─ [4] 배지 대상 추출 완료: {}건 ({}ms)", keysToFlag.size(), time3);

            if (!stepLog.isBadgeDone()) {
                log.info("[Step5] 배지 갱신 시작");
                Instant b1 = Instant.now();
                badgeBatchExecutor.execute(keysToFlag, batchSize);
                badgeTime = Duration.between(b1, Instant.now()).toMillis();
                log.info("[Step5] 배지 갱신 완료 ({}ms)", badgeTime);
                stepLog.setBadgeDone(true);
                stepLogRepository.save(stepLog);
            } else {
                log.info("[Step5] 배지 갱신 스킵: 이미 완료됨");
            }

            if (getAliveWasCount() != total) {
                log.warn("[스케일아웃 감지] 기존 WAS 수={} → 현재 WAS 수={}. 배치 재시작", total, getAliveWasCount());
                runFullBatch(targetDate, batchSize, (int) getAliveWasCount(), index, retryCount + 1);
                return;
            }

            if (!stepLog.isLevelDone()) {
                log.info("[Step6] 등급 갱신 시작");
                Instant l1 = Instant.now();
                userLevelBatchExecutor.execute(users, batchSize);
                levelTime = Duration.between(l1, Instant.now()).toMillis();
                log.info("[Step6] 등급 갱신 완료 ({}ms)", levelTime);
                stepLog.setLevelDone(true);
                stepLogRepository.save(stepLog);
            } else {
                log.info("[Step6] 등급 갱신 스킵: 이미 완료됨");
            }

            if (getAliveWasCount() != total) {
                log.warn("[스케일아웃 감지] 기존 WAS 수={} → 현재 WAS 수={}. 배치 재시작", total, getAliveWasCount());
                runFullBatch(targetDate, batchSize, (int) getAliveWasCount(), index, retryCount + 1);
                return;
            }

            if (!stepLog.isCouponDone()) {
                log.info("[Step7] 쿠폰 발급 시작");
                Instant c1 = Instant.now();
                couponBatchExecutor.execute(users, batchSize);
                couponTime = Duration.between(c1, Instant.now()).toMillis();
                log.info("[Step7] 쿠폰 발급 완료 ({}ms)", couponTime);
                stepLog.setCouponDone(true);
                stepLogRepository.save(stepLog);
            } else {
                log.info("[Step7] 쿠폰 발급 스킵: 이미 완료됨");
            }

        } catch (Exception e) {
            flagManager.endGlobalBadgeBatch();
            log.error("[배치 실패] 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        flagManager.endGlobalBadgeBatch();

        long totalTime = Duration.between(allStart, Instant.now()).toMillis();
        log.info("[전체 배치 완료] 총 소요 시간: {}ms", totalTime);
        log.info(" ├─ [1] 통계 집계: {}ms", time1);
        log.info(" ├─ [2] 유저 조회: {}ms", time2);
        log.info(" ├─ [3] 배지 대상 추출: {}ms", time3);
        log.info(" ├─ [4] 배지 갱신: {}ms", badgeTime);
        log.info(" ├─ [5] 등급 갱신: {}ms", levelTime);
        log.info(" └─ [6] 쿠폰 발급: {}ms", couponTime);
    }

    private long getAliveWasCount() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        return wasInstanceRepository.findAliveInstances(threshold).size();
    }
}
