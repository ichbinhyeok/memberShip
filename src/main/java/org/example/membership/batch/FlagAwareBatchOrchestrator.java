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

        LocalDate date = LocalDate.parse(targetDate);

        // 통계는 한 번만 계산
        Instant t1 = Instant.now();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = jpaOrderService.aggregateUserCategoryStats(date);
        long time1 = Duration.between(t1, Instant.now()).toMillis();
        log.info(" ├─ [1] 통계 집계 완료: {}ms", time1);

        // retry-safe 루프 구조로 변경
        for (int retryCount = 0; retryCount <= MAX_RETRIES; retryCount++) {
            if (retryCount > 0) {
                flagManager.clearAllFlags();
                log.info("[재시작 초기화] 플래그 초기화 완료 (retry={})", retryCount);
            }

            if (flagManager.isBadgeBatchRunning()) {
                log.warn("[중복 실행 차단] 전역 배지 배치 중단됨");
                return;
            }

            flagManager.startGlobalBadgeBatch();
            log.info("[배치 시작] targetDate={}, totalWAS={}, index={}, retry={}", targetDate, total, index, retryCount);

            try {
                Instant allStart = Instant.now();
                long time2 = 0, time3 = 0, badgeTime = 0, levelTime = 0, couponTime = 0;

                // StepLog 확인
                StepExecutionLog stepLog = stepLogRepository
                        .findByTargetDateAndWasIndex(date, index)
                        .orElseGet(() -> {
                            StepExecutionLog log = new StepExecutionLog();
                            log.setTargetDate(date);
                            log.setWasIndex(index);
                            return stepLogRepository.save(log);
                        });

                // 전체 유저 조회 후 분기 필터링
                Instant t2 = Instant.now();
                List<User> users = jpaMembershipService.getAllUsers();
                long fullSize = users.size();

                final ShardUtil shardUtil = new ShardUtil(total, index);

                users = users.stream()
                        .filter(user -> shardUtil.isMine(user.getId()))
                        .toList();

                time2 = Duration.between(t2, Instant.now()).toMillis();
                log.info(" ├─ [2] 유저 필터링 완료: 전체={} → 분기={}(index={}) ({}ms)", fullSize, users.size(), index, time2);

                // 배지 대상 추출
                Instant t3 = Instant.now();
                List<String> keysToFlag = jpaBadgeService.detectBadgeUpdateTargets(users, statMap);
                time3 = Duration.between(t3, Instant.now()).toMillis();
                log.info(" ├─ [3] 배지 대상 추출 완료: {}건 ({}ms)", keysToFlag.size(), time3);

                // Step 1. 배지 갱신
                if (!stepLog.isBadgeDone()) {
                    log.info("[Step4] 배지 갱신 시작");
                    Instant b1 = Instant.now();
                    badgeBatchExecutor.execute(keysToFlag, batchSize);
                    badgeTime = Duration.between(b1, Instant.now()).toMillis();
                    log.info("[Step4] 배지 갱신 완료 ({}ms)", badgeTime);
                    stepLog.setBadgeDone(true);
                    stepLogRepository.save(stepLog);
                } else {
                    log.info("[Step4] 배지 갱신 스킵");
                }

                // 스케일아웃 감지
                long currentAlive = getAliveWasCount();
                if (currentAlive != total) {
                    log.warn("[스케일아웃 감지] 기존 total={} → 현재={}. 재시작", total, currentAlive);
                    total = (int) currentAlive;
                    continue; // retry 루프 재진입
                }

                // Step 2. 등급 갱신
                if (!stepLog.isLevelDone()) {
                    log.info("[Step5] 등급 갱신 시작");
                    Instant l1 = Instant.now();
                    userLevelBatchExecutor.execute(users, batchSize);
                    levelTime = Duration.between(l1, Instant.now()).toMillis();
                    log.info("[Step5] 등급 갱신 완료 ({}ms)", levelTime);
                    stepLog.setLevelDone(true);
                    stepLogRepository.save(stepLog);
                } else {
                    log.info("[Step5] 등급 갱신 스킵");
                }

                // 스케일아웃 재검사
                currentAlive = getAliveWasCount();
                if (currentAlive != total) {
                    log.warn("[스케일아웃 감지] 기존 total={} → 현재={}. 재시작", total, currentAlive);
                    total = (int) currentAlive;
                    continue;
                }

                // Step 3. 쿠폰 발급
                if (!stepLog.isCouponDone()) {
                    log.info("[Step6] 쿠폰 발급 시작");
                    Instant c1 = Instant.now();
                    couponBatchExecutor.execute(users, batchSize);
                    couponTime = Duration.between(c1, Instant.now()).toMillis();
                    log.info("[Step6] 쿠폰 발급 완료 ({}ms)", couponTime);
                    stepLog.setCouponDone(true);
                    stepLogRepository.save(stepLog);
                } else {
                    log.info("[Step6] 쿠폰 발급 스킵");
                }

                flagManager.endGlobalBadgeBatch();

                long totalTime = Duration.between(allStart, Instant.now()).toMillis();
                log.info("[배치 완료] 전체 시간: {}ms", totalTime);
                log.info(" ├─ 유저 필터링: {}ms", time2);
                log.info(" ├─ 배지 대상 추출: {}ms", time3);
                log.info(" ├─ 배지 갱신: {}ms", badgeTime);
                log.info(" ├─ 등급 갱신: {}ms", levelTime);
                log.info(" └─ 쿠폰 발급: {}ms", couponTime);
                return; // 성공 시 루프 종료

            } catch (Exception e) {
                flagManager.endGlobalBadgeBatch();
                log.error("[배치 실패] 예외 발생", e);
                throw new RuntimeException(e);
            }
        }

        log.error("[배치 중단] 최대 {}회 재시도 실패", MAX_RETRIES);
    }

    private long getAliveWasCount() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        return wasInstanceRepository.findAliveInstances(threshold).size();
    }
}
