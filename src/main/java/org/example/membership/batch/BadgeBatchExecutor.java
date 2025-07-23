package org.example.membership.batch;


import lombok.RequiredArgsConstructor;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.common.util.PartitionUtils;
import org.example.membership.service.jpa.JpaBadgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
public class BadgeBatchExecutor {

    private static final Logger log = LoggerFactory.getLogger(BadgeBatchExecutor.class);

    private final JpaBadgeService jpaBadgeService;
    private final FlagManager flagManager;

    public void execute(List<String> keysToUpdate, int batchSize) {
        if (keysToUpdate == null || keysToUpdate.isEmpty()) {
            log.warn("[배지 배치 스킵] 업데이트 대상 없음.");
            return;
        }

        Instant totalStart = Instant.now();

        log.info("[배지 병렬 갱신 시작] 전체 대상: {}개, 배치 크기: {}", keysToUpdate.size(), batchSize);

        // 1. 플래그 등록
        try {
            flagManager.addBadgeFlags(keysToUpdate);
            log.debug("[플래그 등록 완료] 대상 수: {}", keysToUpdate.size());
        } catch (Exception e) {
            log.error("[플래그 등록 실패]", e);
            throw new RuntimeException("플래그 등록 중 오류 발생", e);
        }

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<List<String>> partitions = PartitionUtils.partition(keysToUpdate, 6);
        List<Future<?>> futures = new ArrayList<>();

        log.info("[배지 병렬 처리 분할] 파티션 수: {}", partitions.size());

        for (int i = 0; i < partitions.size(); i++) {
            final int partitionIndex = i;
            final List<String> part = partitions.get(i);

            futures.add(executor.submit(() -> {
                Instant partitionStart = Instant.now();
                int localCount = 0;

                try {
                    for (int start = 0; start < part.size(); start += 1000) {
                        int end = Math.min(start + 1000, part.size());
                        List<String> chunk = part.subList(start, end);
                        if (chunk.isEmpty()) continue;

                        jpaBadgeService.bulkUpdateBadgeStates(chunk, batchSize);
                        localCount += chunk.size();
                    }
                    long duration = Duration.between(partitionStart, Instant.now()).toMillis();
                    log.info("[배지 파티션 완료] #{} 쓰레드: {} | 처리 수: {} | 소요 시간: {}ms",
                            partitionIndex, Thread.currentThread().getName(), localCount, duration);
                } catch (Exception e) {
                    log.error("[배지 파티션 실패] #{} 쓰레드: {} | 키 수: {}", partitionIndex, Thread.currentThread().getName(), part.size(), e);
                    throw e;
                }
                return null;
            }));
        }

        try {
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            executor.shutdownNow();
            log.error("[배지 병렬 갱신 중단] 예외 발생", e);
            throw new RuntimeException("배지 병렬 갱신 중 예외 발생", e);
        } finally {
            executor.shutdown();

            // 3. 플래그 해제
            try {
                for (String key : keysToUpdate) {
                    String[] parts = key.split(":");
                    Long userId = Long.parseLong(parts[0]);
                    Long categoryId = Long.parseLong(parts[1]);
                    flagManager.removeBadgeFlag(userId, categoryId);
                }
                log.debug("[플래그 해제 완료] 대상 수: {}", keysToUpdate.size());
            } catch (Exception e) {
                log.error("[플래그 해제 중 오류 발생]", e);
            }
        }

        long total = Duration.between(totalStart, Instant.now()).toMillis();
        log.info("[병렬 배지 갱신 완료] 전체 대상: {}개 | 총 소요 시간: {}ms", keysToUpdate.size(), total);
    }
}
