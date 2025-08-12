//package org.example.membership.batch;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.membership.common.concurrent.FlagManager;
//import org.example.membership.common.util.PartitionUtils;
//import org.example.membership.config.MyWasInstanceHolder;
//import org.example.membership.entity.batch.BatchExecutionLog;
//import org.example.membership.entity.batch.ChunkExecutionLog;
//import org.example.membership.exception.ScaleOutInterruptedException;
//import org.example.membership.repository.jpa.batch.ChunkExecutionLogRepository;
//import org.example.membership.service.jpa.JpaBadgeService;
//import org.springframework.stereotype.Component;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.stream.Collectors;
//
///**
// * A안: 배지 입력을 (userId, categoryId)로 파싱하여 userId ASC, categoryId ASC로 정렬한 뒤
// * 1000개 단위로 자르고, 범위는 first.userId ~ last.userId로만 기록합니다.
// */
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class BadgeBatchExecutor {
//
//    private static final int PARTITION_THREADS = 6;
//    private static final int CHUNK_SIZE = 1000;
//
//    private final JpaBadgeService jpaBadgeService;
//    private final FlagManager flagManager;
//    private final ChunkExecutionLogRepository chunkExecutionLogRepository;
//    private final MyWasInstanceHolder myWasInstanceHolder;
//
//    // 입력 키: "userId:categoryId"
//    public void execute(List<String> keysToUpdate, int batchSize, BatchExecutionLog batchExecutionLog) {
//        if (keysToUpdate == null || keysToUpdate.isEmpty()) {
//            log.warn("[배지 배치 스킵] 업데이트 대상 없음.");
//            return;
//        }
//
//        log.info("[DEBUG] BadgeBatchExecutor 시작 - keysToUpdate.size={}", keysToUpdate.size());
//        Instant totalStart = Instant.now();
//
//        try {
//            flagManager.addBadgeFlags(keysToUpdate);
//            log.debug("[DEBUG] 플래그 등록 완료 - {}건", keysToUpdate.size());
//        } catch (Exception e) {
//            log.error("[플래그 등록 실패]", e);
//            throw new RuntimeException("플래그 등록 중 오류 발생", e);
//        }
//
//        // 1) 실행ID별 완료 청크 범위 로드(스킵용). 범위키는 "userIdStart-userIdEnd"
//        UUID executionId = batchExecutionLog.getExecutionId();
//        List<ChunkExecutionLog> completedChunks =
//                chunkExecutionLogRepository.findCompletedChunks(executionId, ChunkExecutionLog.StepType.BADGE);
//        Set<String> completedRangeKeys = completedChunks.stream()
//                .map(c -> c.getUserIdStart() + "-" + c.getUserIdEnd())
//                .collect(Collectors.toCollection(HashSet::new));
//
//        // 2) 입력 키 → BadgeKey(userId, categoryId)로 파싱 후 결정론적 정렬
//        List<BadgeKey> sortedKeys = keysToUpdate.stream()
//                .map(BadgeBatchExecutor::parseBadgeKey)
//                .sorted(Comparator
//                        .comparingLong(BadgeKey::userId)
//                        .thenComparingLong(BadgeKey::categoryId))
//                .toList();
//
//        // 3) 파티션 분할
//        ExecutorService executor = Executors.newFixedThreadPool(PARTITION_THREADS);
//        List<List<BadgeKey>> partitions = PartitionUtils.partition(sortedKeys, PARTITION_THREADS);
//        log.info("[DEBUG] 파티션 분할 완료 - 총 파티션 수={}", partitions.size());
//        List<Future<?>> futures = new ArrayList<>();
//
//        for (int i = 0; i < partitions.size(); i++) {
//            final int partitionIndex = i;
//            final List<BadgeKey> part = partitions.get(i);
//            log.info("[DEBUG] 파티션 {} - 처리 키 수={}", partitionIndex, part.size());
//
//            futures.add(executor.submit(() -> {
//                Instant partitionStart = Instant.now();
//                int localCount = 0;
//
//                for (int start = 0; start < part.size(); start += CHUNK_SIZE) {
//                    int end = Math.min(start + CHUNK_SIZE, part.size());
//                    List<BadgeKey> chunk = part.subList(start, end);
//                    if (chunk.isEmpty()) continue;
//
//                    long userIdStart = chunk.get(0).userId();
//                    long userIdEnd   = chunk.get(chunk.size() - 1).userId();
//                    String rangeKey  = userIdStart + "-" + userIdEnd;
//
//                    // 완료 청크 스킵(결정론적 정렬로 항상 동일 범위가 재생성됨)
//                    if (completedRangeKeys.contains(rangeKey)) {
//                        log.info("[청크 스킵] 이미 완료된 범위: {}~{}", userIdStart, userIdEnd);
//                        continue;
//                    }
//
//                    String context = "partition-" + partitionIndex + " chunk " + start + "~" + end;
//
//                    try {
//                        interruptIfNeededInChunk(context);
//
//                        // 서비스는 기존 시그니처를 그대로 사용하므로 문자열 키로 변환
//                        List<String> chunkKeys = toKeyStrings(chunk);
//                        jpaBadgeService.bulkUpdateBadgeStates(chunkKeys, batchSize);
//
//                        localCount += chunk.size();
//                        logChunkExecution(batchExecutionLog, userIdStart, userIdEnd, true);
//
//                    } catch (ScaleOutInterruptedException e) {
//                        logChunkExecution(batchExecutionLog, userIdStart, userIdEnd, false);
//                        throw e;
//
//                    } catch (Exception e) {
//                        logChunkExecution(batchExecutionLog, userIdStart, userIdEnd, false);
//                        log.error("[배지 처리 실패] context={}, chunkSize={}", context, chunk.size(), e);
//                        throw new RuntimeException(e);
//                    }
//                }
//
//                long duration = Duration.between(partitionStart, Instant.now()).toMillis();
//                log.info("[배지 파티션 완료] #{} | 처리 수: {} | 소요 시간: {}ms",
//                        partitionIndex, localCount, duration);
//                return null;
//            }));
//        }
//
//        try {
//            for (Future<?> f : futures) f.get();
//        } catch (Exception e) {
//            executor.shutdownNow();
//            log.error("[배지 병렬 갱신 중단] 예외 발생", e);
//            throw new RuntimeException("배지 병렬 갱신 중 예외 발생", e);
//        } finally {
//            executor.shutdown();
//            // 기존 로직 유지: 모든 키 플래그 해제
//            keysToUpdate.forEach(k -> {
//                try {
//                    String[] parts = k.split(":");
//                    flagManager.removeBadgeFlag(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
//                } catch (Exception e) {
//                    log.error("[플래그 해제 오류] key={}", k, e);
//                }
//            });
//        }
//
//        long total = Duration.between(totalStart, Instant.now()).toMillis();
//        log.info("[병렬 배지 갱신 완료] 전체 대상: {}개 | 총 소요 시간: {}ms", keysToUpdate.size(), total);
//    }
//
//    private void interruptIfNeededInChunk(String context) {
//        if (flagManager.isScaleOutInterrupted()) {
//            log.warn("[인터럽트 감지] 청크 처리 도중 인터럽트 발생. 중단. (context={})", context);
//            throw new ScaleOutInterruptedException("스케일아웃 감지됨: " + context);
//        }
//    }
//
//    private void logChunkExecution(BatchExecutionLog batchExecutionLog,
//                                   long userIdStart,
//                                   long userIdEnd,
//                                   boolean completed) {
//        try {
//            ChunkExecutionLog logEntity = ChunkExecutionLog.builder()
//                    .batchExecutionLog(batchExecutionLog)
//                    .stepType(ChunkExecutionLog.StepType.BADGE)
//                    .wasId(myWasInstanceHolder.getMyUuid())
//                    .recordedAt(LocalDateTime.now())
//                    .userIdStart(userIdStart)
//                    .userIdEnd(userIdEnd)
//                    .completed(completed)
//                    .restored(false)
//                    .build();
//
//            chunkExecutionLogRepository.save(logEntity);
//        } catch (Exception e) {
//            log.error("[청크 로그 기록 실패] completed={} | range={}~{}", completed, userIdStart, userIdEnd, e);
//        }
//    }
//
//    // ========= helpers =========
//
//    // "userId:categoryId" → BadgeKey
//    private static BadgeKey parseBadgeKey(String key) {
//        String[] p = key.split(":");
//        if (p.length != 2) {
//            throw new IllegalArgumentException("잘못된 키 형식: " + key + " (expected userId:categoryId)");
//        }
//        long userId = Long.parseLong(p[0]);
//        long categoryId = Long.parseLong(p[1]);
//        return new BadgeKey(userId, categoryId);
//    }
//
//    // BadgeKey 리스트 → "userId:categoryId" 문자열 리스트
//    private static List<String> toKeyStrings(List<BadgeKey> keys) {
//        List<String> out = new ArrayList<>(keys.size());
//        for (BadgeKey k : keys) {
//            out.add(k.userId() + ":" + k.categoryId());
//        }
//        return out;
//    }
//
//    private record BadgeKey(long userId, long categoryId) {}
//}
