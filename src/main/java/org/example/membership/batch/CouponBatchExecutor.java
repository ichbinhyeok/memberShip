package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.membership.common.util.PartitionUtils;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaCouponService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponBatchExecutor {

    private final JpaCouponService jpaCouponService;

    @Qualifier("batchExecutorService")
    private final ExecutorService executorService;

    public void execute(List<User> users, int batchSize, UUID executionId) {
        if (users == null || users.isEmpty()) {
            log.warn("[쿠폰 발급 스킵] 처리 대상 없음.");
            return;
        }

        log.info("[쿠폰 발급 시작] 대상 수: {} | 배치 크기: {}", users.size(), batchSize);
        Instant totalStart = Instant.now();


        List<List<User>> partitions = PartitionUtils.partition(users, 6);
        List<Future<?>> futures = new ArrayList<>();

        log.info("[쿠폰 발급 분할 처리 시작] 파티션 수: {}", partitions.size());

        for (int i = 0; i < partitions.size(); i++) {
            final int partitionIndex = i;
            final List<User> part = partitions.get(i);

            futures.add(executorService.submit(() -> {
                Instant partitionStart = Instant.now();
                try {
                    jpaCouponService.bulkIssueCoupons(part, batchSize);

                    long duration = Duration.between(partitionStart, Instant.now()).toMillis();
                    log.info("[쿠폰 발급 파티션 완료] #{} | 처리 수: {} | 소요 시간: {}ms",
                            partitionIndex, part.size(), duration);

                } catch (Exception e) {
                    log.error("[쿠폰 발급 파티션 실패] #{} | 처리 수: {}", partitionIndex, part.size(), e);
                    throw new RuntimeException(e);
                }
                return null;
            }));
        }

        try {
            for (Future<?> f : futures) {
                f.get(); // 모든 파티션 작업이 완료될 때까지 대기
            }
        } catch (Exception e) {
            log.error("[쿠폰 발급 중단] 예외 발생", e);
            // executor.shutdownNow();
            throw new RuntimeException("쿠폰 발급 중 예외 발생", e);
        }


        long total = Duration.between(totalStart, Instant.now()).toMillis();
        log.info("[쿠폰 발급 완료] 전체 대상: {} | 총 소요 시간: {}ms", users.size(), total);
    }
}