package org.example.membership.service;

import org.example.membership.entity.BatchExecutionLog;
import org.example.membership.entity.ChunkExecutionLog;
import org.example.membership.entity.WasInstance;
import org.example.membership.repository.jpa.BatchExecutionLogRepository;
import org.example.membership.repository.jpa.ChunkExecutionLogRepository;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.example.membership.service.jpa.ChunkRecoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ChunkRecoveryServiceTest {

    @Autowired
    private ChunkRecoveryService chunkRecoveryService;

    @Autowired
    private ChunkExecutionLogRepository chunkExecutionLogRepository;

    @Autowired
    private BatchExecutionLogRepository batchExecutionLogRepository;

    @Autowired
    private WasInstanceRepository wasInstanceRepository;

    @Test
    void processChunk_updatesRestoredFlag() {
        // given: WasInstance 저장
        UUID wasId = UUID.randomUUID();
        WasInstance was = new WasInstance();
        was.setId(wasId);
        was.setIndexNumber(0);
        was.setLastHeartbeatAt(LocalDateTime.now());
        was.setRegisteredAt(LocalDateTime.now());
        wasInstanceRepository.save(was);

        // given: BatchExecutionLog 저장
        BatchExecutionLog batch = batchExecutionLogRepository.save(
                BatchExecutionLog.builder()
                        .executionId(UUID.randomUUID())
                        .wasInstance(was)
                        .targetDate("2024-01-01")
                        .status(BatchExecutionLog.BatchStatus.RUNNING)
                        .interruptedByScaleOut(false)
                        .build()
        );

        // given: ChunkExecutionLog 저장
        ChunkExecutionLog chunk = chunkExecutionLogRepository.save(ChunkExecutionLog.builder()
                .batchExecutionLog(batch)
                .stepType(ChunkExecutionLog.StepType.BADGE)
                .wasId(wasId)
                .recordedAt(LocalDateTime.now())
                .userIdStart(1L)
                .userIdEnd(10L)
                .completed(false)
                .restored(false)
                .build());

        // when: 복원 처리 수행
        chunkRecoveryService.processChunk(chunk.getId());

        // then: 복원 플래그가 true로 변경되었는지 확인
        ChunkExecutionLog updated = chunkExecutionLogRepository.findById(chunk.getId())
                .orElseThrow();
        assertThat(updated.isRestored()).isTrue();
    }
}
