package org.example.membership.batch;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.util.PartitionUtils;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaCouponService;
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
public class CouponBatchExecutor {

    private static final Logger log = LoggerFactory.getLogger(CouponBatchExecutor.class);

    private final JpaCouponService jpaCouponService;

    public void execute(List<User> users, int batchSize) {
        if (users == null || users.isEmpty()) {
            log.warn("[쿠폰 발급 스킵] 처리 대상 없음.");
            return;
        }

        Instant totalStart = Instant.now();
        log.info("[쿠폰 발급 시작] 대상 수: {} | 배치 크기: {}", users.size(), batchSize);

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<List<User>> partitions = PartitionUtils.partition(users, 6);
        List<Future<?>> futures = new ArrayList<>();

        log.info("[쿠폰 발급 분할 처리 시작] 파티션 수: {}", partitions.size());

        for (int i = 0; i < partitions.size(); i++) {
            final int partitionIndex = i;
            final List<User> part = partitions.get(i);

            futures.add(executor.submit(() -> {
                Instant partitionStart = Instant.now();
                int localCount = 0;
                try {
                    for (int start = 0; start < part.size(); start += 1000) {
                        int end = Math.min(start + 1000, part.size());
                        List<User> chunk = part.subList(start, end);
                        if (chunk.isEmpty()) continue;

                        jpaCouponService.bulkIssueCoupons(chunk, batchSize);
                        localCount += chunk.size();
                    }
                    long duration = Duration.between(partitionStart, Instant.now()).toMillis();
                    log.info("[쿠폰 발급 파티션 완료] #{} 쓰레드: {} | 처리 수: {} | 소요 시간: {}ms",
                            partitionIndex, Thread.currentThread().getName(), localCount, duration);
                } catch (Exception e) {
                    log.error("[쿠폰 발급 파티션 실패] #{} 쓰레드: {} | 키 수: {}", partitionIndex, Thread.currentThread().getName(), part.size(), e);
                    throw e;
                }
                return null;
            }));
        }

        try {
            for (Future<?> f : futures) f.get();
        } catch (Exception e) {
            executor.shutdownNow();
            log.error("[쿠폰 발급 중단] 예외 발생", e);
            throw new RuntimeException("쿠폰 발급 중 예외 발생", e);
        } finally {
            executor.shutdown();
        }

        long total = Duration.between(totalStart, Instant.now()).toMillis();
        log.info("[쿠폰 발급 완료] 전체 대상: {} | 총 소요 시간: {}ms", users.size(), total);
    }
}
