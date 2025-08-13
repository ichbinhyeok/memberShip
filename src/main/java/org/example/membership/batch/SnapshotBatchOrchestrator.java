package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.batch.BatchExecutionLog;
import org.example.membership.entity.batch.BatchExecutionLog.BatchStatus;
import org.example.membership.repository.jpa.batch.BatchExecutionLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotBatchOrchestrator {

    private final BatchExecutionLogRepository batchRepo;
    private final ReadPhaseService readPhaseService;
    private final WritePhaseService writePhaseService;

    @Transactional
    public void runFullBatch(LocalDate targetDate, int batchSize) {
        // (a) 활성 실행 차단
        LocalDateTime nowMinus30s = LocalDateTime.now().minusSeconds(30);
        if (batchRepo.countAliveActiveForTargetDate(
                targetDate.toString(),
                List.of(BatchStatus.RUNNING, BatchStatus.RESTORING),
                nowMinus30s) > 0) {
            log.warn("Active batch already running for {}", targetDate);
            return;
        }


        // (b) RUNNING 생성
        LocalDateTime cutoffAt = targetDate.atStartOfDay();
        BatchExecutionLog logEntity = BatchExecutionLog.create(UUID.randomUUID(), targetDate, cutoffAt, BatchStatus.RUNNING);
        batchRepo.save(logEntity);

        try {
            // (c) 컨텍스트 생성
            CalcContext ctx = readPhaseService.buildContext(targetDate, cutoffAt, batchSize);

            // (d) 빈 컨텍스트 처리
            if (ctx.empty()) {
                logEntity.markCompleted();
                batchRepo.save(logEntity);
                return;
            }

            // (e) 저장 및 반영
            writePhaseService.persistAndApply(logEntity.getExecutionId(), ctx);

            // (f) 쿠폰
            writePhaseService.applyCoupon(logEntity.getExecutionId(), ctx);

            // (g) 완료
            logEntity.markCompleted();
            batchRepo.save(logEntity);

        } catch (Exception e) {
            logEntity.markFailed();
            batchRepo.save(logEntity);
            throw e;
        }
    }

    @Transactional
    public void restoreInterruptedBatch(UUID executionId, LocalDate targetDate, int batchSize) {
        BatchExecutionLog logEntity = batchRepo.findByExecutionId(executionId)
                .orElseThrow(() -> new IllegalArgumentException("BatchExecutionLog not found: " + executionId));

        logEntity.markRestoring();
        batchRepo.save(logEntity);

        try {
            // 기존 산출물 삭제
            writePhaseService.clearResults(executionId);

            // 동일 컨텍스트 생성
            LocalDateTime cutoffAt = targetDate.atStartOfDay();
            CalcContext ctx = readPhaseService.buildContext(targetDate, cutoffAt, batchSize);

            // 저장 및 반영
            writePhaseService.persistAndApply(executionId, ctx);
            writePhaseService.applyCoupon(executionId, ctx);

            logEntity.markCompleted();
            batchRepo.save(logEntity);
        } catch (Exception e) {
            logEntity.markFailed();
            batchRepo.save(logEntity);
            throw e;
        }
    }
}
