package org.example.membership.batch;

import org.example.membership.entity.BatchExecutionLog;
import org.example.membership.entity.ChunkExecutionLog;
import org.example.membership.entity.WasInstance;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.BatchExecutionLogRepository;
import org.example.membership.repository.jpa.ChunkExecutionLogRepository;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BatchRestoreTriggerTest {

    @Autowired
    private BatchRestoreTrigger batchRestoreTrigger;

    @Autowired
    private BatchExecutionLogRepository batchExecutionLogRepository;

    @Autowired
    private ChunkExecutionLogRepository chunkExecutionLogRepository;

    @Autowired
    private WasInstanceRepository wasInstanceRepository;

    @MockBean
    private BadgeBatchExecutor badgeBatchExecutor;

    @MockBean
    private BadgeRepository badgeRepository;

    @Test
    void whenRunningBatchAlive_restoreIsNotTriggered() {
        WasInstance was = wasInstanceRepository.findAll().get(0);

        batchExecutionLogRepository.save(BatchExecutionLog.builder()
                .executionId(UUID.randomUUID())
                .wasInstance(was) // ✅ 수정
                .targetDate("2024-01-01")
                .status(BatchExecutionLog.BatchStatus.RUNNING)
                .interruptedByScaleOut(false)
                .build());

        batchExecutionLogRepository.save(BatchExecutionLog.builder()
                .executionId(UUID.randomUUID())
                .wasInstance(was) // ✅ 수정
                .targetDate("2024-01-01")
                .status(BatchExecutionLog.BatchStatus.INTERRUPTED)
                .interruptedByScaleOut(true)
                .build());

        batchRestoreTrigger.trigger();

        verifyNoInteractions(badgeBatchExecutor);
        assertThat(batchExecutionLogRepository.findRestorableBatches()).hasSize(1);
    }

    @Test
    void restoreBatchProcessesOnlyUnrestoredChunks() {
        WasInstance was = wasInstanceRepository.findAll().get(0);
        was.setLastHeartbeatAt(LocalDateTime.now().minusSeconds(31));
        wasInstanceRepository.saveAndFlush(was);

        BatchExecutionLog batch = batchExecutionLogRepository.save(BatchExecutionLog.builder()
                .executionId(UUID.randomUUID())
                .wasInstance(was) // ✅ 수정
                .targetDate("2024-01-01")
                .status(BatchExecutionLog.BatchStatus.INTERRUPTED)
                .interruptedByScaleOut(true)
                .build());

        // chunk에는 여전히 UUID만 저장하는 구조라고 가정
        chunkExecutionLogRepository.save(ChunkExecutionLog.builder()
                .batchExecutionLog(batch)
                .stepType(ChunkExecutionLog.StepType.BADGE)
                .wasId(was.getId())
                .recordedAt(LocalDateTime.now())
                .userIdStart(1001L)
                .userIdEnd(1020L)
                .completed(false)
                .restored(true)
                .build());

        chunkExecutionLogRepository.save(ChunkExecutionLog.builder()
                .batchExecutionLog(batch)
                .stepType(ChunkExecutionLog.StepType.BADGE)
                .wasId(was.getId())
                .recordedAt(LocalDateTime.now())
                .userIdStart(1021L)
                .userIdEnd(1040L)
                .completed(false)
                .restored(false)
                .build());

        chunkExecutionLogRepository.save(ChunkExecutionLog.builder()
                .batchExecutionLog(batch)
                .stepType(ChunkExecutionLog.StepType.BADGE)
                .wasId(was.getId())
                .recordedAt(LocalDateTime.now())
                .userIdStart(1041L)
                .userIdEnd(1060L)
                .completed(false)
                .restored(false)
                .build());

        when(badgeRepository.findKeysByUserIdRange(1021L, 1040L)).thenReturn(List.of("1021:1"));
        when(badgeRepository.findKeysByUserIdRange(1041L, 1060L)).thenReturn(List.of("1041:1"));

        batchRestoreTrigger.trigger();

        verify(badgeRepository, never()).findKeysByUserIdRange(1001L, 1020L);
        verify(badgeBatchExecutor, times(1)).execute(argThat(keys ->
                        keys.containsAll(List.of("1021:1", "1041:1")) && keys.size() == 2),
                eq(1000), any(BatchExecutionLog.class));

        assertThat(chunkExecutionLogRepository
                .findByBatchExecutionLogAndRestoredFalseOrderByUserIdStartAsc(batch)).isEmpty();
    }
}
