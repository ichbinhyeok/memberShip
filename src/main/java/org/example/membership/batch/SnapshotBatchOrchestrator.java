// SnapshotBatchOrchestrator.java
package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.entity.WasInstance;
import org.example.membership.entity.batch.BatchExecutionLog;
import org.example.membership.exception.ScaleOutInterruptedException;
import org.example.membership.repository.jpa.UserRepository;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.example.membership.repository.jpa.batch.BadgeResultRepository;
import org.example.membership.repository.jpa.batch.BatchExecutionLogRepository;
import org.example.membership.repository.jpa.batch.LevelResultRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotBatchOrchestrator {

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 30;

    private final MyWasInstanceHolder myWasInstanceHolder;
    private final WasInstanceRepository wasInstanceRepository;
    private final BatchExecutionLogRepository batchExecutionLogRepository;
    private final UserRepository userRepository;

    private final BadgeResultRepository badgeResultRepository;
    private final LevelResultRepository levelResultRepository;

    private final ReadPhaseService readPhaseService;   // @Transactional(readOnly = true)
    private final WritePhaseService writePhaseService; // 쓰기/병렬 청크
    private final ScaleOutGuard scaleOutGuard;         // (1) init 호출 추가

    public void runFullBatch(String targetDateStr, int batchSize) {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);

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
        final LocalDateTime cutoffAt = targetDate.atStartOfDay(); // T0
        final int index = myWasInstanceHolder.getMyIndex();
        final int total = myWasInstanceHolder.getTotalWases();

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

        log.info("[신규 배치 시작] executionId={}, targetDate={}, T0={}, index={}, total={}",
                executionId, targetDate, cutoffAt, index, total);

        try {
            long minId = userRepository.findMinUserId();
            long maxId = userRepository.findMaxUserId();

            // 읽기 페이즈: 경계 계산/집계/대상 선별
            CalcContext ctx = readPhaseService.buildContext(
                    targetDate, cutoffAt, minId, maxId, index, total, batchSize
            );

            if (ctx.empty()) {
                log.info("담당 범위 없음. 완료 처리합니다.");
                batchLog.markCompleted();
                batchExecutionLogRepository.save(batchLog);
                return;
            }

            // (1) 쓰기 시작 직전에 가드 초기화
            scaleOutGuard.init(total);

            // 쓰기 페이즈: 저장/반영/쿠폰
            writePhaseService.persistAndApply(executionId, ctx);
            writePhaseService.applyCoupon(executionId, ctx);

            batchLog.markCompleted();
            batchExecutionLogRepository.save(batchLog);
            log.info("[신규 배치 완료] executionId={}", executionId);

        } catch (ScaleOutInterruptedException e) {
            log.warn("[배치 중단] 스케일아웃 감지. executionId={}, msg={}", executionId, e.getMessage());
            batchLog.markInterruptedByScaleOut();
            batchExecutionLogRepository.save(batchLog);
        } catch (Exception e) {
            log.error("[배치 실패] executionId={}", executionId, e);
            batchLog.markFailed();
            batchExecutionLogRepository.save(batchLog);
            throw new RuntimeException(e);
        }
    }

    public void restoreInterruptedBatch(BatchExecutionLog batchLog) {
        final UUID executionId = batchLog.getExecutionId();
        final LocalDateTime cutoffAt = batchLog.getCutoffAt();
        final LocalDate targetDate = LocalDate.parse(batchLog.getTargetDate());
        final int batchSize = 1000;
        final int index = myWasInstanceHolder.getMyIndex();
        final int total = myWasInstanceHolder.getTotalWases();

        log.info("[배치 복원 시작] executionId={}, targetDate={}, T0={}, index={}, total={}",
                executionId, targetDate, cutoffAt, index, total);

        try {
            // (4) 복원 상태 전이
            batchLog.markRestoring();
            batchExecutionLogRepository.save(batchLog);

            // 이전 계산 결과 초기화
            badgeResultRepository.deleteByExecutionId(executionId);
            levelResultRepository.deleteByExecutionId(executionId);

            long minId = userRepository.findMinUserId();
            long maxId = userRepository.findMaxUserId();

            CalcContext ctx = readPhaseService.buildContext(
                    targetDate, cutoffAt, minId, maxId, index, total, batchSize
            );

            if (ctx.empty()) {
                log.info("담당 범위 없음. 복원 스킵");
                batchLog.markCompleted();
                batchExecutionLogRepository.save(batchLog);
                return;
            }

            // (1) 쓰기 시작 직전에 가드 초기화
            scaleOutGuard.init(total);

            writePhaseService.persistAndApply(executionId, ctx);
            writePhaseService.applyCoupon(executionId, ctx);

            batchLog.markCompleted();
            batchExecutionLogRepository.save(batchLog);
            log.info("[배치 복원 완료] executionId={}", executionId);

        } catch (Exception e) {
            log.error("[배치 복원 실패] executionId={}", executionId, e);
            batchLog.markFailed();
            batchExecutionLogRepository.save(batchLog);
            throw new RuntimeException(e);
        }
    }
}
