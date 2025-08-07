package org.example.membership.batch;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.BatchExecutionLog;
import org.example.membership.entity.ChunkExecutionLog;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.BatchExecutionLogRepository;
import org.example.membership.repository.jpa.ChunkExecutionLogRepository;
import org.example.membership.repository.jpa.WasInstanceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchRestoreTrigger {

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 30;

    private final BatchExecutionLogRepository batchExecutionLogRepository;
    private final WasInstanceRepository wasInstanceRepository;
    private final ChunkExecutionLogRepository chunkExecutionLogRepository;
    private final BadgeRepository badgeRepository;
    private final BadgeBatchExecutor badgeBatchExecutor;

    @PostConstruct
    @Transactional
    public void trigger() {
        try {
            List<BatchExecutionLog> candidates = batchExecutionLogRepository.findRestorableBatches();
            if (candidates.isEmpty()) {
                return;
            }

            LocalDateTime threshold = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
            if (wasInstanceRepository.countAliveInstances(threshold) > 0) {
                log.info("다른 WAS 인스턴스가 살아있어 복원 배치를 실행하지 않습니다.");
                return;
            }

            for (BatchExecutionLog logEntry : candidates) {
                int updated = batchExecutionLogRepository.updateStatusToRestoring(logEntry.getId(), LocalDateTime.now());
                if (updated != 1) {
                    continue;
                }

                List<ChunkExecutionLog> chunks = chunkExecutionLogRepository.findRestorableChunks(
                        logEntry, ChunkExecutionLog.StepType.BADGE);

                List<String> keys = new ArrayList<>();
                for (ChunkExecutionLog chunk : chunks) {
                    keys.addAll(badgeRepository.findKeysByUserIdRange(
                            chunk.getUserIdStart(), chunk.getUserIdEnd()));
                }

                if (!keys.isEmpty()) {
                    try {
                        badgeBatchExecutor.execute(keys, 1000, logEntry);

                        // 복원 성공 후에만 플래그 세움
                        for (ChunkExecutionLog chunk : chunks) {
                            chunk.setRestored(true);
                        }
                        chunkExecutionLogRepository.saveAll(chunks);

                        logEntry.markCompleted();
                        batchExecutionLogRepository.save(logEntry);

                    } catch (Exception e) {
                        log.error("[복원 실패] executionId={}, keys={}", logEntry.getExecutionId(), keys.size(), e);
                        // 실패했으면 상태는 그대로 둬서 다음에 재시도 가능하게 유지
                    }
                } else {
                    log.warn("복원할 청크가 없어 COMPLETED 상태로 전환하지 않음. executionId={}", logEntry.getExecutionId());
                }
            }

        } catch (Exception e) {
            log.error("복원 배치 트리거 실행 중 오류", e);
        }
    }
}
