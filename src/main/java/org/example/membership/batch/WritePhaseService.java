package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

// WritePhaseService.java — 핵심만
@Service
@RequiredArgsConstructor
@Slf4j
public class WritePhaseService {

    private final ChunkWriter chunkWriter;
    private final LevelResultCalculator levelResultCalculator;
    @Qualifier("batchExecutorService")
    private final ExecutorService executorService;

    // 1) 배지 산출만 병렬 수행
    public void produceBadgeResults(UUID executionId, CalcContext ctx) {
        if (ctx.empty()) return;
        log.info("[Phase 1] 배지 결과 계산 시작");
        List<Future<?>> fs = new ArrayList<>();
        List<Map.Entry<String,Boolean>> entries = new ArrayList<>(ctx.keysToUpdate().entrySet());
        for (int i = 0; i < entries.size(); i += 500) {
            Map<String,Boolean> sub = entries.subList(i, Math.min(i+500, entries.size()))
                    .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            fs.add(executorService.submit(() -> chunkWriter.writeBadgeChunk(executionId, sub, ctx.batchSize())));
        }
        waitFor(fs);
        log.info("[Phase 1] 배지 결과 계산 완료");
    }

    // 2) 배지 적용
    public void applyBadges(UUID executionId, LocalDateTime t0) {
        log.info("[Phase 2] 배지 결과 적용 시작");
        chunkWriter.applyBadgeResultsPaged(executionId, t0, 5_000);
        log.info("[Phase 2] 배지 결과 적용 완료");
    }

    // 3) 레벨 산출 — 반드시 배지 적용 후 현재 상태 기준으로
    public void produceLevelResults(UUID executionId, CalcContext ctx) {
        log.info("[Phase 3] 레벨 결과 계산 시작");
        levelResultCalculator.calculateAndStoreResults(executionId, ctx.myUsers(), ctx.batchSize()/*, ctx.batchStartTime() 무시*/);
        log.info("[Phase 3] 레벨 결과 계산 완료");
    }

    // 4) 레벨 적용
    public void applyLevels(UUID executionId, LocalDateTime t0) {
        log.info("[Phase 4] 레벨 결과 적용 시작");
        chunkWriter.applyLevelResultsPaged(executionId, t0, 5_000);
        log.info("[Phase 4] 레벨 결과 적용 완료");
    }

    public void applyCoupon(UUID executionId, CalcContext ctx) {
        log.info("[Phase 5] 쿠폰 발급 시작");
        chunkWriter.applyCoupon(executionId, ctx);
        log.info("[Phase 5] 쿠폰 발급 완료");
    }

    private void waitFor(List<Future<?>> fs) {
        for (Future<?> f : fs) try { f.get(); } catch (Exception e) { throw new RuntimeException(e); }
    }
}
