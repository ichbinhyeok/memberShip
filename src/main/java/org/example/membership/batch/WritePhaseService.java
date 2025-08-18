package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WritePhaseService {

    private final ChunkWriter chunkWriter;
    @Qualifier("batchExecutorService")
    private final ExecutorService executorService;

    public void persistAndApply(UUID executionId, CalcContext ctx) {
        if (ctx.empty()) {
            log.info("쓰기 페이즈: 처리할 사용자 없음");
            return;
        }

        try {
            // ==================== 1. 배지 계산 및 적용 ====================
            log.info("[Phase 1] 배지 결과 계산 시작 (병렬 처리)");
            List<Future<?>> badgeFutures = new ArrayList<>();
            List<Map.Entry<String, Boolean>> badgeEntries = new ArrayList<>(ctx.keysToUpdate().entrySet());

            for (int i = 0; i < badgeEntries.size(); i += 500) {
                Map<String, Boolean> subMap = badgeEntries.subList(i, Math.min(i + 500, badgeEntries.size()))
                        .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                badgeFutures.add(executorService.submit(() ->
                        chunkWriter.writeBadgeChunk(executionId, subMap, ctx.batchSize())));
            }
            waitForFutures(badgeFutures);
            log.info("[Phase 1] 배지 결과 계산 완료");

            log.info("[Phase 2] 배지 결과 적용 시작 (단일 트랜잭션)");
            chunkWriter.applyBadgeResultsAll(executionId,ctx.batchStartTime());
            log.info("[Phase 2] 배지 결과 적용 완료");

            // ==================== 2. 레벨 계산 및 적용 ====================
            log.info("[Phase 3] 레벨 결과 계산 시작 (병렬 처리)");
            List<User> userList = ctx.myUsers();
            List<Future<?>> levelFutures = new ArrayList<>();

            for (int i = 0; i < userList.size(); i += 500) {
                List<User> chunk = userList.subList(i, Math.min(i + 500, userList.size()));
                levelFutures.add(executorService.submit(() ->
                        chunkWriter.writeLevelChunk(executionId, chunk, ctx.batchSize(), ctx.batchStartTime())));
            }
            waitForFutures(levelFutures);
            log.info("[Phase 3] 레벨 결과 계산 완료");

            log.info("[Phase 4] 레벨 결과 적용 시작 (단일 트랜잭션)");
            chunkWriter.applyLevelResultsAll(executionId,ctx.batchStartTime());
            log.info("[Phase 4] 레벨 결과 적용 완료");

        } catch (Exception e) {
            throw new RuntimeException("쓰기 페이즈 실패", e);
        }
    }

    private void waitForFutures(List<Future<?>> futures) throws Exception {
        for (Future<?> f : futures) {
            f.get();
        }
    }

    public void applyCoupon(UUID executionId, CalcContext ctx) {
        chunkWriter.applyCoupon(executionId, ctx);
    }
}