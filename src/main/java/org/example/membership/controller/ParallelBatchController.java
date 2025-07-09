package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.util.PartitionUtils;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.service.jpa.JpaBadgeService;
import org.example.membership.service.jpa.JpaCouponService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.jpa.JpaOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Controller for running JPA batch jobs in parallel.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/batch/jpa/parallel")
public class ParallelBatchController {

    private static final Logger log = LoggerFactory.getLogger(ParallelBatchController.class);

    private final JpaOrderService jpaOrderService;
    private final JpaBadgeService jpaBadgeService;
    private final JpaMembershipService jpaMembershipService;
    private final JpaCouponService jpaCouponService;
    private final BadgeRepository badgeRepository;

    /**
     * Run all JPA batch jobs in parallel.
     */
    @PostMapping("/full")
    public void runFullBatch(@RequestParam String targetDate,
                             @RequestParam(defaultValue = "100") int batchSize) {
        LocalDate date = LocalDate.parse(targetDate);
        Instant allStart = Instant.now();

        // 1. 주문 통계 집계
        Instant t1 = Instant.now();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = runOrderBatch(date);
        long time1 = Duration.between(t1, Instant.now()).toMillis();

        // 2. 유저 전체 조회
        Instant t2 = Instant.now();
        List<User> users = jpaMembershipService.getAllUsers();
        long time2 = Duration.between(t2, Instant.now()).toMillis();

        // 3. 배지 갱신
        Instant t3 = Instant.now();
        runParallelBadgeBatch(users, statMap, batchSize);
        long time3 = Duration.between(t3, Instant.now()).toMillis();

        // 4. 등급 갱신
        Instant t4 = Instant.now();
        runParallelUserLevelBatch(users, batchSize);
        long time4 = Duration.between(t4, Instant.now()).toMillis();

        // 5. 쿠폰 발급
        Instant t5 = Instant.now();
        runParallelCouponBatch(users, batchSize);
        long time5 = Duration.between(t5, Instant.now()).toMillis();

        long total = Duration.between(allStart, Instant.now()).toMillis();

        log.info("[전체 배치 완료] 총 소요 시간: {}ms", total);
        log.info(" ├─ [1] 주문 통계 집계: {}ms", time1);
        log.info(" ├─ [2] 유저 조회: {}ms", time2);
        log.info(" ├─ [3] 배지 갱신: {}ms", time3);
        log.info(" ├─ [4] 등급 갱신: {}ms", time4);
        log.info(" └─ [5] 쿠폰 발급: {}ms", time5);
    }

    /**
     * Run badge update in parallel.
     */
    @PostMapping("/badges")
    public void runBadge(@RequestParam String targetDate,
                         @RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = runOrderBatch(LocalDate.parse(targetDate));
        runParallelBadgeBatch(users, statMap, batchSize);
    }

    /**
     * Run membership level update in parallel.
     */
    @PostMapping("/users")
    public void runUserLevel(@RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        runParallelUserLevelBatch(users, batchSize);
    }

    /**
     * Run coupon issuance in parallel.
     */
    @PostMapping("/coupons")
    public void runCoupon(@RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        runParallelCouponBatch(users, batchSize);
    }

    // ===== 내부 로직 =====
    private Map<Long, Map<Long, OrderCountAndAmount>> runOrderBatch(LocalDate targetDate) {
        Instant start = Instant.now();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = jpaOrderService.aggregateUserCategoryStats(targetDate);
        log.info("[1] 주문 통계 집계 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
        return statMap;
    }

    private void runParallelBadgeBatch(List<User> users,
                                       Map<Long, Map<Long, OrderCountAndAmount>> statMap,
                                       int batchSize) {
        Instant totalStart = Instant.now();
        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<List<User>> partitions = PartitionUtils.partition(users, 6);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < partitions.size(); i++) {
            final List<User> part = partitions.get(i);
            final int threadNo = i + 1;
            futures.add(executor.submit(() -> {
                Instant t = Instant.now();
                log.info("[Thread-{}] 배지 갱신 시작 (유저 {}명)", threadNo, part.size());
                jpaBadgeService.bulkUpdateBadgeStates(part, statMap, batchSize);
                long time = Duration.between(t, Instant.now()).toMillis();
                log.info("[Thread-{}] 배지 갱신 완료: {}ms", threadNo, time);
            }));
        }

        try {
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            executor.shutdownNow();
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }

        long total = Duration.between(totalStart, Instant.now()).toMillis();
        log.info("[병렬 배지 갱신 완료] 전체 소요 시간: {}ms", total);
    }

    private void runParallelUserLevelBatch(List<User> users, int batchSize) {
        Instant totalStart = Instant.now();

        List<Object[]> counts = badgeRepository.countActiveBadgesGroupedByUserId();
        Map<Long, Long> activeBadgeMap = new HashMap<>();
        for (Object[] row : counts) {
            Long userId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            activeBadgeMap.put(userId, count);
        }

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<List<User>> partitions = PartitionUtils.partition(users, 6);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < partitions.size(); i++) {
            final List<User> part = partitions.get(i);
            final int threadNo = i + 1;
            futures.add(executor.submit(() -> {
                Instant t = Instant.now();
                log.info("[Thread-{}] 등급 갱신 시작 (유저 {}명)", threadNo, part.size());
                jpaMembershipService.bulkUpdateMembershipLevelsAndLog(part, activeBadgeMap, batchSize);
                long time = Duration.between(t, Instant.now()).toMillis();
                log.info("[Thread-{}] 등급 갱신 완료: {}ms", threadNo, time);
            }));
        }

        try {
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            executor.shutdownNow();
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }

        long total = Duration.between(totalStart, Instant.now()).toMillis();
        log.info("[병렬 등급 갱신 완료] 전체 소요 시간: {}ms", total);
    }

    private void runParallelCouponBatch(List<User> users, int batchSize) {
        Instant totalStart = Instant.now();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<List<User>> partitions = PartitionUtils.partition(users, 6);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < partitions.size(); i++) {
            final List<User> part = partitions.get(i);
            final int threadNo = i + 1;
            futures.add(executor.submit(() -> {
                Instant t = Instant.now();
                log.info("[Thread-{}] 쿠폰 발급 시작 (유저 {}명)", threadNo, part.size());
                jpaCouponService.bulkIssueCoupons(part, batchSize);
                long time = Duration.between(t, Instant.now()).toMillis();
                log.info("[Thread-{}] 쿠폰 발급 완료: {}ms", threadNo, time);
            }));
        }

        try {
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            executor.shutdownNow();
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }

        long total = Duration.between(totalStart, Instant.now()).toMillis();
        log.info("[병렬 쿠폰 발급 완료] 전체 소요 시간: {}ms", total);
    }
}
