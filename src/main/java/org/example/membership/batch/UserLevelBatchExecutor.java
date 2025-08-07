package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.common.util.PartitionUtils;
import org.example.membership.config.MyWasInstanceHolder;
import org.example.membership.entity.*;
import org.example.membership.exception.ScaleOutInterruptedException;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.ChunkExecutionLogRepository;
import org.example.membership.service.jpa.JpaMembershipService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserLevelBatchExecutor {

    private final JpaMembershipService jpaMembershipService;
    private final BadgeRepository badgeRepository;
    private final ChunkExecutionLogRepository chunkExecutionLogRepository;
    private final FlagManager flagManager;
    private final MyWasInstanceHolder myWasInstanceHolder;

    public void execute(List<User> users, int batchSize, BatchExecutionLog batchExecutionLog) {
        if (users == null || users.isEmpty()) {
            log.warn("[등급 갱신 스킵] 처리 대상 없음.");
            return;
        }

        log.info("[등급 갱신 시작] 대상 수: {} | 배치 크기: {}", users.size(), batchSize);
        Instant totalStart = Instant.now();

        //  완료된 청크 스킵용 데이터 조회
        UUID executionId = batchExecutionLog.getExecutionId();
        List<ChunkExecutionLog> completedChunks = chunkExecutionLogRepository
                .findCompletedChunks(executionId, ChunkExecutionLog.StepType.LEVEL);
        Set<String> completedRangeKeys = new HashSet<>();
        for (ChunkExecutionLog log : completedChunks) {
            completedRangeKeys.add(log.getUserIdStart() + "-" + log.getUserIdEnd());
        }

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<List<User>> partitions = PartitionUtils.partition(users, 6);
        List<Future<?>> futures = new ArrayList<>();

        log.info("[등급 갱신 분할 처리 시작] 파티션 수: {}", partitions.size());

        for (int i = 0; i < partitions.size(); i++) {
            final int partitionIndex = i;
            final List<User> part = partitions.get(i);

            futures.add(executor.submit(() -> {
                Instant partitionStart = Instant.now();
                int localCount = 0;
                try {
                    for (int start = 0; start < part.size(); start += 1000) {
                        int end = Math.min(start + 1000, part.size());
                        List<User> chunk = part.subList(start, end);
                        if (chunk.isEmpty()) continue;

                        long userIdStart = chunk.get(0).getId();
                        long userIdEnd = chunk.get(chunk.size() - 1).getId();
                        String rangeKey = userIdStart + "-" + userIdEnd;

                        // 완료된 청크 스킵
                        if (completedRangeKeys.contains(rangeKey)) {
                            log.info("[청크 스킵] 등급 청크 이미 완료됨: {}~{}", userIdStart, userIdEnd);
                            continue;
                        }

                        String context = "partition-" + partitionIndex + " chunk " + start + "~" + end;
                        interruptIfNeededInChunk(context);

                        Map<Long, Long> badgeCountMap = getActiveBadgeCountMap(chunk);
                        jpaMembershipService.bulkUpdateMembershipLevelsAndLog(chunk, badgeCountMap, batchSize);
                        localCount += chunk.size();

                        logChunkExecution(batchExecutionLog, userIdStart, userIdEnd, true);
                    }

                    long duration = Duration.between(partitionStart, Instant.now()).toMillis();
                    log.info("[등급 갱신 파티션 완료] #{} 쓰레드: {} | 처리 수: {} | 소요 시간: {}ms",
                            partitionIndex, Thread.currentThread().getName(), localCount, duration);
                } catch (ScaleOutInterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("[등급 갱신 파티션 실패] #{} 쓰레드: {} | 키 수: {}", partitionIndex, Thread.currentThread().getName(), part.size(), e);
                    throw new RuntimeException(e);
                }
                return null;
            }));
        }

        try {
            for (Future<?> f : futures) f.get();
        } catch (Exception e) {
            executor.shutdownNow();
            log.error("[등급 갱신 중단] 예외 발생", e);
            throw new RuntimeException("등급 갱신 중 예외 발생", e);
        } finally {
            executor.shutdown();
        }

        long total = Duration.between(totalStart, Instant.now()).toMillis();
        log.info("[등급 갱신 완료] 전체 대상: {} | 총 소요 시간: {}ms", users.size(), total);
    }

    private void interruptIfNeededInChunk(String context) {
        if (flagManager.isScaleOutInterrupted()) {
            log.warn("[인터럽트 감지] 등급 청크 처리 중단. context={}", context);
            throw new ScaleOutInterruptedException("스케일아웃 감지됨: " + context);
        }
    }

    private Map<Long, Long> getActiveBadgeCountMap(List<User> users) {
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        List<Object[]> counts = badgeRepository.countActiveBadgesGroupedByUserIds(userIds);

        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : counts) {
            Long userId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            map.put(userId, count);
        }
        return map;
    }

    private void logChunkExecution(BatchExecutionLog batchExecutionLog, long userIdStart, long userIdEnd, boolean completed) {
        try {
            ChunkExecutionLog log = ChunkExecutionLog.builder()
                    .batchExecutionLog(batchExecutionLog)
                    .stepType(ChunkExecutionLog.StepType.LEVEL)
                    .wasId(myWasInstanceHolder.getMyUuid())
                    .recordedAt(LocalDateTime.now())
                    .userIdStart(userIdStart)
                    .userIdEnd(userIdEnd)
                    .completed(completed)
                    .restored(false)
                    .build();

            chunkExecutionLogRepository.save(log);
        } catch (Exception e) {
            log.error("[등급 청크 로그 기록 실패] range={}~{} completed={}", userIdStart, userIdEnd, completed, e);
        }
    }
}
