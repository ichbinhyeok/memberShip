package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.common.util.PartitionUtils;
import org.example.membership.entity.User;
import org.example.membership.entity.batch.BatchExecutionLog;
import org.example.membership.exception.ScaleOutInterruptedException;
import org.example.membership.service.jpa.JpaCouponService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponBatchExecutor {

    private final JpaCouponService jpaCouponService;
    private final FlagManager flagManager; // 스케일아웃 감지를 위해 유지

    /*
     * LEGACY: 스냅샷 아키텍처에서는 ChunkExecutionLog를 통한 복구 대신,
     * JpaCouponService의 coupon_issue_log를 통해 멱등성을 보장하는 것이 더 단순하고 강력합니다.
     * 따라서 관련 의존성과 로직을 주석 처리합니다.
     *
    private final ChunkExecutionLogRepository chunkExecutionLogRepository;
    private final MyWasInstanceHolder myWasInstanceHolder;
    */

    public void execute(List<User> users, int batchSize, UUID executionId) {
        if (users == null || users.isEmpty()) {
            log.warn("[쿠폰 발급 스킵] 처리 대상 없음.");
            return;
        }

        log.info("[쿠폰 발급 시작] 대상 수: {} | 배치 크기: {}", users.size(), batchSize);
        Instant totalStart = Instant.now();

        /*
         * LEGACY: ChunkExecutionLog를 이용한 청크 단위 스킵 로직
         *
        UUID executionId = batchExecutionLog.getExecutionId();
        List<ChunkExecutionLog> completedChunks = chunkExecutionLogRepository
                .findCompletedChunks(executionId, ChunkExecutionLog.StepType.COUPON);
        Set<String> completedRangeKeys = new HashSet<>();
        for (ChunkExecutionLog log : completedChunks) {
            completedRangeKeys.add(log.getUserIdStart() + "-" + log.getUserIdEnd());
        }
        */

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<List<User>> partitions = PartitionUtils.partition(users, 6);
        List<Future<?>> futures = new ArrayList<>();

        log.info("[쿠폰 발급 분할 처리 시작] 파티션 수: {}", partitions.size());

        for (int i = 0; i < partitions.size(); i++) {
            final int partitionIndex = i;
            final List<User> part = partitions.get(i);

            futures.add(executor.submit(() -> {
                Instant partitionStart = Instant.now();
                try {
                    /*
                     * LEGACY: 청크 단위로 나누는 로직은 유지하되, 스킵 및 로그 기록 로직은 주석 처리합니다.
                     *
                    for (int start = 0; start < part.size(); start += 1000) {
                        int end = Math.min(start + 1000, part.size());
                        List<User> chunk = part.subList(start, end);
                        if (chunk.isEmpty()) continue;

                        long userIdStart = chunk.get(0).getId();
                        long userIdEnd = chunk.get(chunk.size() - 1).getId();
                        String rangeKey = userIdStart + "-" + userIdEnd;

                        if (completedRangeKeys.contains(rangeKey)) {
                            log.info("[청크 스킵] 쿠폰 청크 이미 완료됨: {}~{}", userIdStart, userIdEnd);
                            continue;
                        }
                    */
                    String context = "partition-" + partitionIndex;
                    interruptIfNeededInChunk(context);

                    // 서비스 레이어에서 중복 발급을 방지한다고 가정하고, 파티션 단위로 발급 실행
                    jpaCouponService.bulkIssueCoupons(part, batchSize);

                    /*
                        logChunkExecution(batchExecutionLog, userIdStart, userIdEnd, true);
                    }
                    */

                    long duration = Duration.between(partitionStart, Instant.now()).toMillis();
                    log.info("[쿠폰 발급 파티션 완료] #{} | 처리 수: {} | 소요 시간: {}ms",
                            partitionIndex, part.size(), duration);

                } catch (ScaleOutInterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    // logChunkExecution(batchExecutionLog, -1, -1, false);
                    log.error("[쿠폰 발급 파티션 실패] #{} | 키 수: {}", partitionIndex, part.size(), e);
                    throw new RuntimeException(e);
                }
                return null;
            }));
        }

        try {
            for (Future<?> f : futures) f.get();
        } catch (Exception e) {
            executor.shutdownNow();
            log.error("[쿠폰 발급 중단] 예외 발생", e);
            throw new RuntimeException("쿠폰 발급 중 예외 발생", e);
        } finally {
            executor.shutdown();
        }

        long total = Duration.between(totalStart, Instant.now()).toMillis();
        log.info("[쿠폰 발급 완료] 전체 대상: {} | 총 소요 시간: {}ms", users.size(), total);
    }

    private void interruptIfNeededInChunk(String context) {
        if (flagManager.isScaleOutInterrupted()) {
            log.warn("[인터럽트 감지] 쿠폰 청크 처리 중단. context={}", context);
            throw new ScaleOutInterruptedException("스케일아웃 감지됨: " + context);
        }
    }

    /*
     * LEGACY: ChunkExecutionLog 기록 로직
     *
    private void logChunkExecution(BatchExecutionLog batchExecutionLog, long userIdStart, long userIdEnd, boolean completed) {
        try {
            ChunkExecutionLog log = ChunkExecutionLog.builder()
                    .batchExecutionLog(batchExecutionLog)
                    .stepType(ChunkExecutionLog.StepType.COUPON)
                    .wasId(myWasInstanceHolder.getMyUuid())
                    .recordedAt(LocalDateTime.now())
                    .userIdStart(userIdStart)
                    .userIdEnd(userIdEnd)
                    .completed(completed)
                    .restored(false)
                    .build();

            chunkExecutionLogRepository.save(log);
        } catch (Exception e) {
            log.error("[쿠폰 청크 로그 기록 실패] range={}~{} completed={}", userIdStart, userIdEnd, completed, e);
        }
    }
    */
}
