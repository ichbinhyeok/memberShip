package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    //  1. 배지 결과 저장 역할만 수행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeBadgeChunk(UUID executionId, Map<String, Boolean> subKeys, int batchSize) {
        badgeResultCalculator.calculateAndStoreResults(executionId, subKeys, batchSize);
    }

    //  2. 레벨 결과 저장 역할만 수행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeLevelChunk(UUID executionId, List<User> users, int batchSize, LocalDateTime batchStartTime) {
        levelResultCalculator.calculateAndStoreResults(executionId, users, batchSize, batchStartTime);
    }

    // 3. 배지 결과만 적용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyBadgeResultsAll(UUID executionId, LocalDateTime batchStartTime) {
        batchResultApplier.applyBadgeResults(executionId, batchStartTime);
    }

    //  4. 레벨 결과만 적용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyLevelResultsAll(UUID executionId, LocalDateTime batchStartTime) {
        batchResultApplier.applyLevelResults(executionId, batchStartTime);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyCoupon(UUID executionId, CalcContext ctx) {
        if (ctx.empty()) return;
        couponBatchExecutor.execute(ctx.myUsers(), ctx.batchSize(), executionId);
    }
}
