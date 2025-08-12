package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.util.ShardUtil;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.User;
import org.example.membership.entity.WasInstance;
import org.example.membership.entity.batch.BatchExecutionLog;
import org.example.membership.exception.ScaleOutInterruptedException;
import org.example.membership.repository.jpa.UserRepository;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.example.membership.repository.jpa.batch.BadgeResultRepository;
import org.example.membership.repository.jpa.batch.BatchExecutionLogRepository;
import org.example.membership.repository.jpa.batch.LevelResultRepository;
import org.example.membership.service.jpa.JpaBadgeService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.jpa.JpaOrderService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
public class SnapshotBatchOrchestrator {

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 30;

    // --- 의존성 ---
    private final JpaOrderService jpaOrderService;
    private final JpaBadgeService jpaBadgeService;
    private final JpaMembershipService jpaMembershipService;
    private final UserRepository userRepository;

    // 계산 및 반영 컴포넌트
    private final BadgeResultCalculator badgeResultCalculator;
    private final LevelResultCalculator levelResultCalculator;
    private final CouponBatchExecutor couponBatchExecutor; // 쿠폰은 일단 유지
    private final BatchResultApplier batchResultApplier;

    // 리포지토리
    private final WasInstanceRepository wasInstanceRepository;
    private final BatchExecutionLogRepository batchExecutionLogRepository;
    private final BadgeResultRepository badgeResultRepository;
    private final LevelResultRepository levelResultRepository;

    private final MyWasInstanceHolder myWasInstanceHolder;



    /**
     * 새로운 배치를 처음부터 실행합니다.
     */
    // 현재 활성화된 배치가 있나 검사.
    public void runFullBatch(String targetDateStr, int batchSize) {

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);

        // --- 1. 준비 단계 (Preparation) ---
        long aliveCount = batchExecutionLogRepository.countAliveActiveForTargetDate(
                targetDateStr,
                BatchExecutionLog.BatchStatus.RUNNING,
                BatchExecutionLog.BatchStatus.RESTORING,
                threshold
        );
        if (aliveCount > 0) {
            log.warn("[보류] targetDate={} 살아있는 활성 배치 존재", targetDateStr);
            return;
        }

        WasInstance was = wasInstanceRepository.findById(myWasInstanceHolder.getMyUuid())
                .orElseThrow(() -> new IllegalStateException("WAS 인스턴스 정보 없음"));

        final UUID executionId = UUID.randomUUID();
        final LocalDate targetDate = LocalDate.parse(targetDateStr);
        final LocalDateTime cutoffAt = targetDate.atStartOfDay(); // T0: 매달 00시

        BatchExecutionLog batchLog = batchExecutionLogRepository.save(
                BatchExecutionLog.builder()
                        .executionId(executionId)
                        .wasInstance(was)
                        .targetDate(targetDateStr)
                        .cutoffAt(cutoffAt)
                        .status(BatchExecutionLog.BatchStatus.RUNNING)
                        .interruptedByScaleOut(false)
                        .build()
        );

        log.info("[신규 배치 시작] executionId={}, targetDate={}, T0={}", executionId, targetDate, cutoffAt);

        try {
            // 계산 및 반영 로직 실행
            runCalculationAndReflection(batchLog, targetDate, cutoffAt, batchSize);

            // 최종 완료 처리
            batchLog.markCompleted();
            batchExecutionLogRepository.save(batchLog);
            log.info("[신규 배치 완료] executionId={}", executionId);

        } catch (ScaleOutInterruptedException e) {
            log.warn("[배치 중단] 스케일아웃 감지. executionId={}, message={}", executionId, e.getMessage());
            batchLog.markInterruptedByScaleOut();
            batchExecutionLogRepository.save(batchLog);
        } catch (Exception e) {
            log.error("[배치 실패] 예외 발생. executionId={}", executionId, e);
            batchLog.markFailed();
            batchExecutionLogRepository.save(batchLog);
            throw new RuntimeException(e);
        }
    }

    /**
     * 중단된 배치를 복원합니다.
     */
    @Transactional
    public void restoreInterruptedBatch(BatchExecutionLog batchLog) {
        final UUID executionId = batchLog.getExecutionId();
        final LocalDateTime cutoffAt = batchLog.getCutoffAt();
        final LocalDate targetDate = LocalDate.parse(batchLog.getTargetDate());
        final int batchSize = 1000; // 기본 배치 사이즈 사용

        log.info("[배치 복원 시작] executionId={}, targetDate={}, T0={}", executionId, targetDate, cutoffAt);

        try {
            // --- 1. 이전 결과 초기화 (Wipe) ---
            log.info(" ├─ [1] 이전 계산 결과 초기화...");
            badgeResultRepository.deleteByExecutionId(executionId);
            levelResultRepository.deleteByExecutionId(executionId);
            log.info(" ├─ [1] 초기화 완료.");

            // --- 2. 계산 및 반영 재실행 (Recalculate & Reflect) ---
            runCalculationAndReflection(batchLog, targetDate, cutoffAt, batchSize);

            // 최종 완료 처리
            batchLog.markCompleted();
            batchExecutionLogRepository.save(batchLog);
            log.info("[배치 복원 완료] executionId={}", executionId);

        } catch (Exception e) {
            log.error("[배치 복원 실패] 예외 발생. executionId={}", executionId, e);
            batchLog.markFailed();
            batchExecutionLogRepository.save(batchLog);
            throw new RuntimeException(e);
        }
    }

    /**
     * 계산과 반영의 핵심 로직을 수행하는 공통 메서드
     */
    @Transactional(readOnly = true)
    public void runCalculationAndReflection(BatchExecutionLog batchLog, LocalDate targetDate, LocalDateTime cutoffAt, int batchSize) {
        final UUID executionId = batchLog.getExecutionId();
        final int index = myWasInstanceHolder.getMyIndex();
        final int total = myWasInstanceHolder.getTotalWases();
        Instant t1 = Instant.now();

        // 1) 경계 계산(범위 분할)
        long minId = userRepository.findMinUserId();
        long maxId = userRepository.findMaxUserId();

        long totalSpan = maxId - minId + 1;
        long rangeSize = Math.max(1, (long) Math.ceil((double) totalSpan / total));
        long rangeStart = minId + (long) index * rangeSize;
        long rangeEnd   = (index == total - 1) ? maxId : Math.min(maxId, rangeStart + rangeSize - 1);

        if (rangeStart > maxId) {
            log.info("이 샤드는 담당 범위가 없습니다. index={}, total={}", index, total);
            return;
        }

        log.info(" ├─ 유저 범위 계산: {} ~ {} (size≈{}), total={}, index={}",
                rangeStart, rangeEnd, rangeSize, total, index);

        Map<Long, Map<Long, OrderCountAndAmount>> statMap =
                jpaOrderService.aggregateUserCategoryStats(targetDate, cutoffAt, rangeStart, rangeEnd);
        log.info(" ├─ 통계 집계 완료: {}ms", Duration.between(t1, Instant.now()).toMillis());


        // 2) 범위 조회
        List<User> myUsers = userRepository.findUsersInRange(rangeStart, rangeEnd);
        log.info(" ├─ 유저 범위 필터링: {} ~ {} → {}명", rangeStart, rangeEnd, myUsers.size());

// 분산 샤드
//        List<User> users = jpaMembershipService.getAllUsers();
//        ShardUtil shardUtil = new ShardUtil(total, index);
//        List<User> myUsers = users.stream().filter(u -> shardUtil.isMine(u.getId())).toList();
//        log.info(" ├─ 유저 필터링: 전체={} → 분기={}", users.size(), myUsers.size());

        //  반환 타입을 Map<String, Boolean>으로 변경
        Map<String, Boolean> keysToUpdate = jpaBadgeService.detectBadgeUpdateTargets(myUsers, statMap);
        log.info(" ├─ 배지 대상 추출: {}건", keysToUpdate.size());

        badgeResultCalculator.calculateAndStoreResults(executionId, keysToUpdate, batchSize);
        currentAliveCheck(total);

        levelResultCalculator.calculateAndStoreResults(executionId, myUsers, batchSize);
        currentAliveCheck(total);

        // --- 반영 단계 (Reflection) ---
        log.info(" ├─ 계산 완료. 최종 결과 반영 시작...");
        batchResultApplier.applyResults(executionId);
        log.info(" ├─ 결과 반영 완료.");

        // --- 쿠폰 지급 단계 ---
        //  일관성을 위해 executionId를 전달하도록 변경
        couponBatchExecutor.execute(myUsers, batchSize, executionId);
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
