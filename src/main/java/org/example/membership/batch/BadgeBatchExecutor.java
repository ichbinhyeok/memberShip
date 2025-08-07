package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.common.util.PartitionUtils;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.entity.BatchExecutionLog;
import org.example.membership.entity.ChunkExecutionLog;
import org.example.membership.exception.ScaleOutInterruptedException;
import org.example.membership.repository.jpa.ChunkExecutionLogRepository;
import org.example.membership.service.jpa.JpaBadgeService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BadgeBatchExecutor {

    private final JpaBadgeService jpaBadgeService;
    private final FlagManager flagManager;
    private final ChunkExecutionLogRepository chunkExecutionLogRepository;
    private final MyWasInstanceHolder myWasInstanceHolder;

    public void execute(List<String> keysToUpdate, int batchSize, BatchExecutionLog batchExecutionLog) {
        if (keysToUpdate == null || keysToUpdate.isEmpty()) {
            log.warn("[배지 배치 스킵] 업데이트 대상 없음.");
            return;
        }

        log.info("[DEBUG] BadgeBatchExecutor 시작 - keysToUpdate.size={}", keysToUpdate.size());
        Instant totalStart = Instant.now();

        try {
            flagManager.addBadgeFlags(keysToUpdate);
            log.debug("[DEBUG] 플래그 등록 완료 - {}건", keysToUpdate.size());
        } catch (Exception e) {
            log.error("[플래그 등록 실패]", e);
            throw new RuntimeException("플래그 등록 중 오류 발생", e);
        }

        // ✅ 완료된 청크 조회
        UUID executionId = batchExecutionLog.getExecutionId();
        List<ChunkExecutionLog> completedChunks = chunkExecutionLogRepository.findCompletedChunks(executionId, ChunkExecutionLog.StepType.BADGE);
        Set<String> completedRangeKeys = completedChunks.stream()
                .map(log -> log.getUserIdStart() + "-" + log.getUserIdEnd())
                .collect(Collectors.toSet());

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<List<String>> partitions = PartitionUtils.partition(keysToUpdate, 6);
        log.info("[DEBUG] 파티션 분할 완료 - 총 파티션 수={}", partitions.size());
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < partitions.size(); i++) {
            final int partitionIndex = i;
            final List<String> part = partitions.get(i);
            log.info("[DEBUG] 파티션 {} - 처리 키 수={}", partitionIndex, part.size());

            futures.add(executor.submit(() -> {
                Instant partitionStart = Instant.now();
                int localCount = 0;

                for (int start = 0; start < part.size(); start += 1000) {
                    int end = Math.min(start + 1000, part.size());
                    List<String> chunk = part.subList(start, end);

                    long userIdStart = extractMinUserId(chunk);
                    long userIdEnd = extractMaxUserId(chunk);
                    String rangeKey = userIdStart + "-" + userIdEnd;

                    // ✅ 스킵 로직 적용
                    if (completedRangeKeys.contains(rangeKey)) {
                        log.info("[청크 스킵] 이미 완료된 범위: {}~{}", userIdStart, userIdEnd);
                        continue;
                    }

                    String context = "partition-" + partitionIndex + " chunk " + start + "~" + end;

                    try {
                        interruptIfNeededInChunk(context);

                        jpaBadgeService.bulkUpdateBadgeStates(chunk, batchSize);
                        localCount += chunk.size();
                        logChunkExecution(batchExecutionLog, chunk, true);

                    } catch (ScaleOutInterruptedException e) {
                        logChunkExecution(batchExecutionLog, chunk, false);
                        throw e;

                    } catch (Exception e) {
                        logChunkExecution(batchExecutionLog, chunk, false);
                        log.error("[배지 처리 실패] context={}, chunkSize={}", context, chunk.size(), e);
                        throw new RuntimeException(e);
                    }
                }

                long duration = Duration.between(partitionStart, Instant.now()).toMillis();
                log.info("[배지 파티션 완료] #{} | 처리 수: {} | 소요 시간: {}ms",
                        partitionIndex, localCount, duration);
                return null;
            }));
        }

        try {
            for (Future<?> f : futures) f.get();
        } catch (Exception e) {
            executor.shutdownNow();
            log.error("[배지 병렬 갱신 중단] 예외 발생", e);
            throw new RuntimeException("배지 병렬 갱신 중 예외 발생", e);
        } finally {
            executor.shutdown();
            keysToUpdate.forEach(k -> {
                try {
                    String[] parts = k.split(":");
                    flagManager.removeBadgeFlag(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
                } catch (Exception e) {
                    log.error("[플래그 해제 오류] key={}", k, e);
                }
            });
        }

        long total = Duration.between(totalStart, Instant.now()).toMillis();
        log.info("[병렬 배지 갱신 완료] 전체 대상: {}개 | 총 소요 시간: {}ms", keysToUpdate.size(), total);
    }

    private void interruptIfNeededInChunk(String context) {
        if (flagManager.isScaleOutInterrupted()) {
            log.warn("[인터럽트 감지] 청크 처리 도중 인터럽트 발생. 중단. (context={})", context);
            throw new ScaleOutInterruptedException("스케일아웃 감지됨: " + context);
        }
    }

    private void logChunkExecution(BatchExecutionLog batchExecutionLog, List<String> chunk, boolean completed) {
        try {
            long userIdStart = extractMinUserId(chunk);
            long userIdEnd = extractMaxUserId(chunk);

            ChunkExecutionLog log = ChunkExecutionLog.builder()
                    .batchExecutionLog(batchExecutionLog)
                    .stepType(ChunkExecutionLog.StepType.BADGE)
                    .wasId(myWasInstanceHolder.getMyUuid())
                    .recordedAt(LocalDateTime.now())
                    .userIdStart(userIdStart)
                    .userIdEnd(userIdEnd)
                    .completed(completed)
                    .restored(false)
                    .build();

            chunkExecutionLogRepository.save(log);
        } catch (Exception e) {
            log.error("[청크 로그 기록 실패] completed={} | chunkSize={}", completed, chunk.size(), e);
        }
    }

    private long extractMinUserId(List<String> chunk) {
        return chunk.stream()
                .map(k -> Long.parseLong(k.split(":")[0]))
                .min(Long::compare)
                .orElseThrow(() -> new IllegalArgumentException("chunk에 userId 없음"));
    }

    private long extractMaxUserId(List<String> chunk) {
        return chunk.stream()
                .map(k -> Long.parseLong(k.split(":")[0]))
                .max(Long::compare)
                .orElseThrow(() -> new IllegalArgumentException("chunk에 userId 없음"));
    }
}
