// org/example/membership/batch/SnapshotBatchOrchestrator.java
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotBatchOrchestrator {

    private final BatchExecutionLogRepository batchRepo;
    private final ReadPhaseService readPhaseService;
    private final WritePhaseService writePhaseService;

    /**
     * 선점 성공하여 실제 배치를 수행하면 true, 선점 실패면 false.
     * 예외가 발생해도 선점은 성공한 것이므로 true를 반환(스케줄러가 플래그 해제 브로드캐스트).
     */
    @Transactional
    public boolean runFullBatch(LocalDate targetDate, int batchSize) {
        UUID executionId = UUID.randomUUID();
        LocalDateTime cutoffAt = targetDate.atStartOfDay();

        int inserted = batchRepo.insertIfNotRunning(executionId, targetDate.toString(), cutoffAt);
        if (inserted == 0) {
            return false; // 다른 WAS가 실행 중
        }

        BatchExecutionLog logEntity = batchRepo.findByExecutionId(executionId)
                .orElseThrow(() -> new IllegalStateException("Execution record not found after insert"));

        try {
            var ctx = readPhaseService.buildContext(targetDate, cutoffAt, batchSize);
            if (ctx.empty()) {
                logEntity.markCompleted();
                batchRepo.save(logEntity);
                return true;
            }

            writePhaseService.persistAndApply(logEntity.getExecutionId(), ctx);
            writePhaseService.applyCoupon(logEntity.getExecutionId(), ctx);

            logEntity.markCompleted();
            batchRepo.save(logEntity);
            return true;

        } catch (Exception e) {
            logEntity.markFailed();
            batchRepo.save(logEntity);
            log.error("[배지 배치] 실패 executionId={}", executionId, e);
            return true;
        }
    }
}
