// ChunkWriter.java
package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    public void writeBadgeChunk(UUID executionId, Map<String, Boolean> subKeys, int batchSize) {
        badgeResultCalculator.calculateAndStoreResults(executionId, subKeys, batchSize);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeLevelChunk(UUID executionId, List<User> users, int batchSize) {
        levelResultCalculator.calculateAndStoreResults(executionId, users, batchSize);
    }

    // 최신 스냅샷에서 페이지로 읽고, 저장은 내부 REQUIRES_NEW로 커밋
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, isolation = Isolation.READ_COMMITTED)
    public void applyBadgeResultsPaged(UUID executionId, LocalDateTime batchStartTime, int pageSize) {
        batchResultApplier.applyBadgeResultsPaged(executionId, batchStartTime, pageSize);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, isolation = Isolation.READ_COMMITTED)
    public void applyLevelResultsPaged(UUID executionId, LocalDateTime batchStartTime, int pageSize) {
        batchResultApplier.applyLevelResultsPaged(executionId, batchStartTime, pageSize);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyCoupon(UUID executionId, CalcContext ctx) {
        if (ctx.empty()) return;
        couponBatchExecutor.execute(ctx.myUsers(), ctx.batchSize(), executionId);
    }
}
