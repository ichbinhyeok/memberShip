// WritePhaseService.java
package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WritePhaseService {

    private final ChunkWriter chunkWriter;
    private final ScaleOutGuard scaleOutGuard;

    public void persistAndApply(UUID executionId, CalcContext ctx) {
        if (ctx.empty()) {
            log.info("담당 범위 없음. 쓰기 페이즈 스킵");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(6, r -> {
            Thread t = new Thread(r, "batch-chunk-");
            t.setDaemon(true);
            return t;
        });

        try {
            // 사용자 ID → 청크(500 고정)
            List<Long> userIds = ctx.myUsers().stream().map(User::getId).toList();
            List<List<Long>> chunks = sliceFixed(userIds, 500);

            List<Future<?>> futures = new ArrayList<>();
            for (List<Long> chunk : chunks) {
                futures.add(executor.submit(() -> {
                    chunkWriter.writeBadgeAndLevelChunk(executionId, chunk, ctx, scaleOutGuard);
                    return null;
                }));
            }

            for (Future<?> f : futures) {
                f.get();
            }

            // 반영은 범위 단위(필요 시 여기도 청크화 가능)
            chunkWriter.applyResultsRange(executionId, ctx.rangeStart(), ctx.rangeEnd(), scaleOutGuard);

        } catch (Exception e) {
            executor.shutdownNow();
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void applyCoupon(UUID executionId, CalcContext ctx) {
        if (ctx.empty()) return;
        chunkWriter.applyCoupon(executionId, ctx); // (2) 실제 지급 수행
    }

    private static <T> List<List<T>> sliceFixed(List<T> list, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            out.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return out;
    }
}
