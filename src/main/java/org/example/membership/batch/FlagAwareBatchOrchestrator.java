package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.common.util.ShardUtil;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.*;
import org.example.membership.exception.ScaleOutInterruptedException;
import org.example.membership.repository.jpa.*;
import org.example.membership.service.jpa.*;
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
    private final BatchExecutionLogRepository batchExecutionLogRepository;

    private final MyWasInstanceHolder myWasInstanceHolder;

    public void runFullBatch(String targetDate, int batchSize) {
        UUID myUuid = myWasInstanceHolder.getMyUuid();
        int index = myWasInstanceHolder.getMyIndex();
        int total = myWasInstanceHolder.getTotalWases();

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);

        long aliveRunningCount = batchExecutionLogRepository.countRunningWithAliveHeartbeat(threshold);
        if (aliveRunningCount > 0) {
            log.warn("[복원 보류] 아직 살아있는 RUNNING 상태의 배치 존재");
            return;
        }

        BatchExecutionLog batchLog = batchExecutionLogRepository.save(
                BatchExecutionLog.builder()
                        .executionId(UUID.randomUUID())
                        .wasId(myUuid)
                        .targetDate(targetDate)
                        .status(BatchExecutionLog.BatchStatus.RUNNING)
                        .interruptedByScaleOut(false)
                        .build()
        );

        log.info("[DEBUG] runFullBatch 시작 - targetDate={}, batchSize={}, myUuid={}", targetDate, batchSize, myUuid);

        WasInstance self = wasInstanceRepository.findById(myUuid)
                .orElseThrow(() -> new IllegalStateException("WAS 인스턴스 정보 없음"));

        log.info("[DEBUG] WAS 인덱스 정보: index={}, totalWAS={}", index, total);

        LocalDate date = LocalDate.parse(targetDate);

        Instant t1 = Instant.now();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = jpaOrderService.aggregateUserCategoryStats(date);
        long time1 = Duration.between(t1, Instant.now()).toMillis();
        log.info(" ├─ [1] 통계 집계 완료: {}ms", time1);

        for (int retryCount = 0; retryCount <= MAX_RETRIES; retryCount++) {
            StepExecutionLog stepLog = null;
            boolean completed = false;

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

                stepLog = stepLogRepository
                        .findByTargetDateAndWasIndex(date, index)
                        .orElseGet(() -> {
                            StepExecutionLog log = new StepExecutionLog();
                            log.setTargetDate(date);
                            log.setWasIndex(index);
                            return stepLogRepository.save(log);
                        });

                List<User> users = jpaMembershipService.getAllUsers();
                long fullSize = users.size();
                log.info("[DEBUG] 전체 유저 수: {}", fullSize);

                final ShardUtil shardUtil = new ShardUtil(total, index);
                users = users.stream().filter(user -> shardUtil.isMine(user.getId())).toList();
                log.info("[DEBUG] 분기 유저 수 (index={}): {}", index, users.size());

                long time2 = Duration.between(t1, Instant.now()).toMillis();
                log.info(" ├─ [2] 유저 필터링 완료: 전체={} → 분기={}(index={}) ({}ms)", fullSize, users.size(), index, time2);

                Instant t3 = Instant.now();
                List<String> keysToFlag = jpaBadgeService.detectBadgeUpdateTargets(users, statMap);
                long time3 = Duration.between(t3, Instant.now()).toMillis();
                log.info(" ├─ [3] 배지 대상 추출 완료: {}건 ({}ms)", keysToFlag.size(), time3);

                if (!stepLog.isBadgeDone()) {
                    log.info("[Step4] 배지 갱신 시작");
                    badgeBatchExecutor.execute(keysToFlag, batchSize,batchLog);
                } else {
                    log.info("[Step4] 배지 갱신 스킵");
                }

                currentAliveCheck(total);

                if (!stepLog.isLevelDone()) {
                    log.info("[Step5] 등급 갱신 시작");
                    userLevelBatchExecutor.execute(users, batchSize,batchLog);
                } else {
                    log.info("[Step5] 등급 갱신 스킵");
                }

                currentAliveCheck(total);

                if (!stepLog.isCouponDone()) {
                    log.info("[Step6] 쿠폰 발급 시작");
                    couponBatchExecutor.execute(users, batchSize,batchLog);
                } else {
                    log.info("[Step6] 쿠폰 발급 스킵");
                }

                completed = true;
                batchLog.markCompleted();
                batchExecutionLogRepository.save(batchLog);

                long totalTime = Duration.between(allStart, Instant.now()).toMillis();
                log.info("[배치 완료] 전체 시간: {}ms", totalTime);
                return;

            } catch (ScaleOutInterruptedException e) {
                log.warn("[배치 중단] 스케일아웃 인터럽트 감지됨: {}", e.getMessage());
                if (stepLog != null) {
                    stepLog.setInterrupted(true);
                    stepLogRepository.save(stepLog);
                }
                batchLog.markInterrupted();
                batchExecutionLogRepository.save(batchLog);
                flagManager.resetScaleOutInterruptFlag();
                throw e;

            } catch (Exception e) {
                log.error("[배치 실패] 예외 발생", e);
                throw new RuntimeException(e);

            } finally {
                if (!completed) {
                    flagManager.endGlobalBadgeBatch();
                    if (batchLog.getStatus() == BatchExecutionLog.BatchStatus.RUNNING) {
                        batchLog.markInterrupted();
                        batchExecutionLogRepository.save(batchLog);
                    }
                }
            }
        }

        log.error("[배치 중단] 최대 {}회 재시도 실패", MAX_RETRIES);
    }

    private void currentAliveCheck(long total) {
        long currentAlive = getAliveWasCount();
        if (currentAlive != total) {
            log.warn("[스케일아웃 감지] 기존 total={} → 현재={}. 재시작", total, currentAlive);
            throw new ScaleOutInterruptedException("WAS 수 변경 감지됨: 기존=" + total + ", 현재=" + currentAlive);
        }
    }

    private long getAliveWasCount() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        return wasInstanceRepository.findAliveInstances(threshold).size();
    }
}
