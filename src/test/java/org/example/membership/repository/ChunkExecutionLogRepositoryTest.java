package org.example.membership.repository;

import org.example.membership.entity.BatchExecutionLog;
import org.example.membership.entity.ChunkExecutionLog;
import org.example.membership.entity.WasInstance;
import org.example.membership.repository.jpa.BatchExecutionLogRepository;
import org.example.membership.repository.jpa.ChunkExecutionLogRepository;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ChunkExecutionLogRepositoryTest {

    @Autowired
    private ChunkExecutionLogRepository chunkExecutionLogRepository;

    @Autowired
    private BatchExecutionLogRepository batchExecutionLogRepository;

    @Autowired
    private WasInstanceRepository wasInstanceRepository;

    @Test
    void findUnrestoredUncompletedChunksOrderByUserIdStart_returnsOnlyMatchingChunksInOrder() {
        // 1. 선행: WasInstance 저장
        UUID wasId = UUID.randomUUID();
        WasInstance was = new WasInstance();
        was.setId(wasId);
        was.setIndexNumber(1);
        was.setLastHeartbeatAt(LocalDateTime.now());
        was.setIp("127.0.0.1");
        was.setPort(8080);
        was.setHostname("test-host");
        wasInstanceRepository.save(was);

        // 2. 배치 로그 저장
        BatchExecutionLog batch = batchExecutionLogRepository.save(
                BatchExecutionLog.builder()
                        .executionId(UUID.randomUUID())
                        .wasInstance(was)
                        .targetDate("2024-01-01")
                        .status(BatchExecutionLog.BatchStatus.RUNNING)
                        .interruptedByScaleOut(false)
                        .build()
        );

        // 3. 조건 만족 청크 2개
        chunkExecutionLogRepository.save(ChunkExecutionLog.builder()
                .batchExecutionLog(batch)
                .stepType(ChunkExecutionLog.StepType.BADGE)
                .wasId(UUID.randomUUID())
                .recordedAt(LocalDateTime.now())
                .userIdStart(1L)
                .userIdEnd(10L)
                .completed(false)
                .restored(false)
                .build());

        chunkExecutionLogRepository.save(ChunkExecutionLog.builder()
                .batchExecutionLog(batch)
                .stepType(ChunkExecutionLog.StepType.BADGE)
                .wasId(UUID.randomUUID())
                .recordedAt(LocalDateTime.now())
                .userIdStart(11L)
                .userIdEnd(20L)
                .completed(false)
                .restored(false)
                .build());

        // 4. 조건 불일치 청크 3개
        chunkExecutionLogRepository.save(ChunkExecutionLog.builder()
                .batchExecutionLog(batch)
                .stepType(ChunkExecutionLog.StepType.BADGE)
                .wasId(UUID.randomUUID())
                .recordedAt(LocalDateTime.now())
                .userIdStart(21L)
                .userIdEnd(30L)
                .completed(true)
                .restored(false)
                .build());

        chunkExecutionLogRepository.save(ChunkExecutionLog.builder()
                .batchExecutionLog(batch)
                .stepType(ChunkExecutionLog.StepType.BADGE)
                .wasId(UUID.randomUUID())
                .recordedAt(LocalDateTime.now())
                .userIdStart(31L)
                .userIdEnd(40L)
                .completed(false)
                .restored(true)
                .build());

        chunkExecutionLogRepository.save(ChunkExecutionLog.builder()
                .batchExecutionLog(batch)
                .stepType(ChunkExecutionLog.StepType.BADGE)
                .wasId(UUID.randomUUID())
                .recordedAt(LocalDateTime.now())
                .userIdStart(41L)
                .userIdEnd(50L)
                .completed(true)
                .restored(true)
                .build());

        // 5. 검증
        List<ChunkExecutionLog> result = chunkExecutionLogRepository
                .findUnrestoredUncompletedChunksOrderByUserIdStart(batch);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserIdStart()).isEqualTo(1L);
        assertThat(result.get(1).getUserIdStart()).isEqualTo(11L);
    }
}
