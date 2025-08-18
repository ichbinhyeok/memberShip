package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.batch.BatchExecutionLog;
import org.example.membership.repository.jpa.batch.BatchExecutionLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotBatchOrchestrator {

    private final BatchExecutionLogRepository batchRepo;
    private final ReadPhaseService readPhaseService;
    private final WritePhaseService writePhaseService;

    @Transactional
    public boolean runFullBatch(LocalDate targetDate, int batchSize) {
        UUID executionId = UUID.randomUUID();
        LocalDateTime batchStartTime = LocalDateTime.now();
        LocalDateTime cutoffAt = targetDate.atStartOfDay();

        int inserted = batchRepo.insertIfNotRunning(executionId, targetDate.toString(), cutoffAt);
        if (inserted == 0) {
            log.warn("다른 배치가 실행 중이므로 현재 배치를 건너뜁니다.");
            return false;
        }

        BatchExecutionLog logEntity = batchRepo.findByExecutionId(executionId)
                .orElseThrow(() -> new IllegalStateException("Execution record not found after insert"));

        try {
            CalcContext ctx = readPhaseService.buildContext(targetDate, cutoffAt, batchSize, batchStartTime);
            if (ctx.empty()) {
                log.info("처리할 대상이 없어 배치를 완료합니다. executionId={}", executionId);
                logEntity.markCompleted();
                batchRepo.save(logEntity);
                return true;
            }

            writePhaseService.persistAndApply(logEntity.getExecutionId(), ctx);
            writePhaseService.applyCoupon(logEntity.getExecutionId(), ctx);

            logEntity.markCompleted();
            batchRepo.save(logEntity);
            log.info("배치 실행이 성공적으로 완료되었습니다. executionId={}", executionId);
            return true;

        } catch (Exception e) {
            logEntity.markFailed();
            batchRepo.save(logEntity);
            log.error("[배지 배치] 실패 executionId={}", executionId, e);
            return true;
        }
    }
}