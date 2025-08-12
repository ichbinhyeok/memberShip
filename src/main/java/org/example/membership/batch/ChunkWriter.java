// ChunkWriter.java
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
    private final CouponBatchExecutor couponBatchExecutor; // (2) 주입

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeBadgeAndLevelChunk(UUID executionId,
                                        List<Long> userIdChunk,
                                        CalcContext ctx,
                                        ScaleOutGuard guard) {
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
            if (allow.contains(uid)) subKeys.put(k, e.getValue());
        }

        badgeResultCalculator.calculateAndStoreResults(executionId, subKeys, ctx.batchSize());
        levelResultCalculator.calculateAndStoreResults(executionId, users, ctx.batchSize());

        guard.ensureUnchanged();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyResultsRange(UUID executionId,
                                  long startUserId,
                                  long endUserId,
                                  ScaleOutGuard guard) {
        batchResultApplier.applyResults(executionId, startUserId, endUserId);
        guard.ensureUnchanged();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyCoupon(UUID executionId, CalcContext ctx) {
        if (ctx.empty()) return;
        // (2) 쿠폰 지급 실행
        couponBatchExecutor.execute(ctx.myUsers(), ctx.batchSize(), executionId);
    }
}
