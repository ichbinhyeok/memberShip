package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.batch.BadgeResultRepository;
import org.example.membership.repository.jpa.batch.LevelResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WritePhaseService {

    private final ChunkWriter chunkWriter;
    private final BadgeResultRepository badgeResultRepository;
    private final LevelResultRepository levelResultRepository;

    public void persistAndApply(UUID executionId, CalcContext ctx) {
        if (ctx.empty()) {
            log.info("쓰기 페이즈: 처리할 사용자 없음");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(6, r -> {
            Thread t = new Thread(r, "batch-chunk-");
            t.setDaemon(true);
            return t;
        });

        try {
            List<Long> userIds = ctx.myUsers().stream().map(User::getId).toList();
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < userIds.size(); i += 500) {
                List<Long> chunk = userIds.subList(i, Math.min(i + 500, userIds.size()));
                futures.add(executor.submit(() ->
                        chunkWriter.writeBadgeAndLevelChunk(executionId, chunk, ctx)));
            }

            for (Future<?> f : futures) {
                f.get(); // 완료 대기
            }

            chunkWriter.applyResultsAll(executionId);

        } catch (Exception e) {
            executor.shutdownNow();
            throw new RuntimeException("쓰기 페이즈 실패", e);
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
        chunkWriter.applyCoupon(executionId, ctx);
    }

    @Transactional
    public void clearResults(UUID executionId) {
        log.info("Clearing results for executionId={}", executionId);
        badgeResultRepository.deleteByExecutionId(executionId);
        levelResultRepository.deleteByExecutionId(executionId);
    }
}
