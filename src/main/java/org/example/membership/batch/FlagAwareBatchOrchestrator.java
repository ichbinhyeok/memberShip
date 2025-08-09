package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.common.util.ShardUtil;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.BatchExecutionLog;
import org.example.membership.entity.BatchExecutionLog.BatchStatus;
import org.example.membership.entity.StepExecutionLog;
import org.example.membership.entity.User;
import org.example.membership.entity.WasInstance;
import org.example.membership.exception.ScaleOutInterruptedException;
import org.example.membership.repository.jpa.BatchExecutionLogRepository;
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
    private final BatchExecutionLogRepository batchExecutionLogRepository;

    private final MyWasInstanceHolder myWasInstanceHolder;

    public void runFullBatch(String targetDate, int batchSize) {
        final UUID myUuid = myWasInstanceHolder.getMyUuid();
        final int index = myWasInstanceHolder.getMyIndex();
        final int total = myWasInstanceHolder.getTotalWases();

        // 활성 배치(해당 날짜) 존재 시 보류 // 배치를 하고 싶다면 INTERRUPTED상태 즉 배치가 다 멈춘 상태에서만(정합성)
        long activeForDate = batchExecutionLogRepository.countActiveForTargetDate(
                targetDate, BatchStatus.RUNNING, BatchStatus.RESTORING);
        if (activeForDate > 0) {
            log.warn("[보류] targetDate={} 활성 배치 존재", targetDate);
            return;
        }

        WasInstance was = wasInstanceRepository.findById(myUuid)
                .orElseThrow(() -> new IllegalStateException("WAS 인스턴스 정보 없음"));

        BatchExecutionLog batchLog = batchExecutionLogRepository.save(
                BatchExecutionLog.builder()
                        .executionId(UUID.randomUUID())
                        .wasInstance(was)
                        .targetDate(targetDate)
                        .status(BatchStatus.RUNNING)
                        .interruptedByScaleOut(false)
                        .build()
        );

        LocalDate date = LocalDate.parse(targetDate);
        log.info("[runFullBatch] targetDate={}, batchSize={}, wasIndex={}/{}", targetDate, batchSize, index, total);

        // 전역 게이트
        boolean gateTurnedOnHere = false;
        if (!flagManager.isGlobalApiGateOn()) {
            flagManager.turnOnGlobalApiGate();
            gateTurnedOnHere = true;
            log.info("[게이트 ON]");
        }

        // 오케스트레이터 재진입 방지
        if (!flagManager.tryStartOrchestratorRun()) {
            log.warn("[중복 실행 차단] 동일 WAS에서 오케스트레이터 실행 중");
            return;
        }

        boolean completed = false;

        try {
            Instant t1 = Instant.now();
            Map<Long, Map<Long, OrderCountAndAmount>> statMap = jpaOrderService.aggregateUserCategoryStats(date);
            log.info(" ├─ [1] 통계 집계 완료: {}ms", Duration.between(t1, Instant.now()).toMillis());

            for (int retry = 0; retry <= MAX_RETRIES; retry++) {
                if (retry > 0) {
                    flagManager.clearTransientFlags();
                    log.info("[재시작 초기화] retry={}", retry);
                }

                try {
                    StepExecutionLog stepLog = stepLogRepository
                            .findByTargetDateAndWasIndex(date, index)
                            .orElseGet(() -> {
                                StepExecutionLog log = new StepExecutionLog();
                                log.setTargetDate(date);
                                log.setWasIndex(index);
                                return stepLogRepository.save(log);
                            });

                    List<User> users = jpaMembershipService.getAllUsers();
                    long fullSize = users.size();
                    ShardUtil shardUtil = new ShardUtil(total, index);
                    users = users.stream().filter(u -> shardUtil.isMine(u.getId())).toList();
                    log.info(" ├─ [2] 유저 필터링: 전체={} → 분기={}", fullSize, users.size());

                    List<String> keysToFlag = jpaBadgeService.detectBadgeUpdateTargets(users, statMap);
                    log.info(" ├─ [3] 배지 대상 추출: {}건", keysToFlag.size());

                    if (!stepLog.isBadgeDone()) {
                        badgeBatchExecutor.execute(keysToFlag, batchSize, batchLog);
                        stepLog.setBadgeDone(true);
                        stepLogRepository.save(stepLog);
                        stepLogRepository.flush();
                    }


                    currentAliveCheck(total);

                    if (!stepLog.isLevelDone()) {
                        userLevelBatchExecutor.execute(users, batchSize, batchLog);
                        stepLog.setLevelDone(true);
                        stepLogRepository.save(stepLog);
                        stepLogRepository.flush();
                    }

                    currentAliveCheck(total);

                    if (!stepLog.isCouponDone()) {
                        couponBatchExecutor.execute(users, batchSize, batchLog);
                        stepLog.setCouponDone(true);
                        stepLogRepository.save(stepLog);
                        stepLogRepository.flush();
                    }


                    completed = true;
                    batchLog.markCompleted();
                    batchExecutionLogRepository.save(batchLog);

                    log.info("[배치 완료] elapsed={}ms", Duration.between(t1, Instant.now()).toMillis());
                    break;

                } catch (ScaleOutInterruptedException e) {
                    log.warn("[중단] 스케일아웃 감지: {}", e.getMessage());
                    batchLog.markInterruptedByScaleOut();
                    batchExecutionLogRepository.save(batchLog);
                    return; // 전역 게이트 유지, 복원 트리거가 이어서 진입

                } catch (Exception e) {
                    log.error("[실패] 예외 발생(retry={})", retry, e);
                    if (retry == MAX_RETRIES) {
                        batchLog.markFailed();
                        batchExecutionLogRepository.save(batchLog);
                        throw new RuntimeException(e);
                    }
                    // 다음 루프로 재시도
                }
            }

        } finally {
            flagManager.endOrchestratorRun();

            if (completed) {
                if (gateTurnedOnHere) {
                    flagManager.turnOffGlobalApiGate();
                    log.info("[게이트 OFF]");
                }
            } else {
                if (batchLog.getStatus() == BatchStatus.RUNNING) {
                    batchLog.markFailed();
                    batchExecutionLogRepository.save(batchLog);
                }
            }
        }
    }

    private void currentAliveCheck(long total) {
        long currentAlive = getAliveWasCount();
        if (currentAlive != total) {
            throw new ScaleOutInterruptedException("WAS 수 변경 감지: 기존=" + total + ", 현재=" + currentAlive);
        }
    }

    private long getAliveWasCount() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        return wasInstanceRepository.findAliveInstances(threshold).size();
    }
}
