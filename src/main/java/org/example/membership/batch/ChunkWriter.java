package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkWriter {

    private final BadgeResultCalculator badgeResultCalculator;
    private final LevelResultCalculator levelResultCalculator;
    private final BatchResultApplier batchResultApplier;
    private final CouponBatchExecutor couponBatchExecutor;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeBadgeAndLevelChunk(UUID executionId,
                                        List<Long> userIdChunk,
                                        CalcContext ctx) {
        if (userIdChunk.isEmpty()) return;

        Set<Long> allow = new HashSet<>(userIdChunk);
        List<User> users = ctx.myUsers().stream()
                .filter(u -> allow.contains(u.getId()))
                .toList();

        Map<String, Boolean> subKeys = new HashMap<>();
        for (Map.Entry<String, Boolean> e : ctx.keysToUpdate().entrySet()) {
            String k = e.getKey();
            int idx = k.indexOf(':');
            long uid = Long.parseLong(k.substring(0, idx));
            if (allow.contains(uid)) {
                subKeys.put(k, e.getValue());
            }
        }

        // 배지 결과 저장
        badgeResultCalculator.calculateAndStoreResults(executionId, subKeys, ctx.batchSize());

        // 레벨 결과 저장
        levelResultCalculator.calculateAndStoreResults(executionId, users, ctx.batchSize());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyResultsAll(UUID executionId) {
        batchResultApplier.applyResults(executionId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyCoupon(UUID executionId, CalcContext ctx) {
        if (ctx.empty()) return;
        couponBatchExecutor.execute(ctx.myUsers(), ctx.batchSize(), executionId);
    }
}
