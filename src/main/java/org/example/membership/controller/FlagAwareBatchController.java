package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.concurrent.FlagManager;
import org.example.membership.common.util.PartitionUtils;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.service.jpa.JpaBadgeService;
import org.example.membership.service.jpa.JpaCouponService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.jpa.JpaOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequiredArgsConstructor
@RequestMapping("/batch/jpa/flag-aware")
public class FlagAwareBatchController {

    private static final Logger log = LoggerFactory.getLogger(FlagAwareBatchController.class);

    private final JpaOrderService jpaOrderService;
    private final JpaBadgeService jpaBadgeService;
    private final JpaMembershipService jpaMembershipService;
    private final JpaCouponService jpaCouponService;
    private final BadgeRepository badgeRepository;
    private final FlagManager flagManager;

    @PostMapping("/full")
    public void runFullBatch(@RequestParam String targetDate,
                             @RequestParam(defaultValue = "100") int batchSize) {
        if (flagManager.isBadgeBatchRunning()) {
            log.warn("[전체 배치 중복 실행 차단] 이미 전역 배지 배치가 진행 중입니다.");
            return;
        }

        flagManager.startGlobalBadgeBatch();
        try {
            LocalDate date = LocalDate.parse(targetDate);
            Instant allStart = Instant.now();

            Instant t1 = Instant.now();
            Map<Long, Map<Long, OrderCountAndAmount>> statMap = jpaOrderService.aggregateUserCategoryStats(date);
            long time1 = Duration.between(t1, Instant.now()).toMillis();

            Instant t2 = Instant.now();
            List<User> users = jpaMembershipService.getAllUsers();
            long time2 = Duration.between(t2, Instant.now()).toMillis();

            Instant t3 = Instant.now();
            List<String> keysToFlag = jpaBadgeService.detectBadgeUpdateTargets(users, statMap);

            long time3 = Duration.between(t3, Instant.now()).toMillis();

            flagManager.endGlobalBadgeBatch();

            Instant t4 = Instant.now();
            runParallelBadgeBatch(keysToFlag, batchSize);
            long time4 = Duration.between(t4, Instant.now()).toMillis();

            Instant t5 = Instant.now();
            runParallelUserLevelBatch(users, batchSize);
            long time5 = Duration.between(t5, Instant.now()).toMillis();

            Instant t6 = Instant.now();
            runParallelCouponBatch(users, batchSize);
            long time6 = Duration.between(t6, Instant.now()).toMillis();

            long total = Duration.between(allStart, Instant.now()).toMillis();

            log.info("[전체 배치 완료] 총 소요 시간: {}ms", total);
            log.info(" ├─ [1] 주문 통계 집계: {}ms", time1);
            log.info(" ├─ [2] 유저 조회: {}ms", time2);
            log.info(" ├─ [3] 배지 갱신 대상 추출 및 플래그 설정: {}ms", time3);
            log.info(" ├─ [4] 배지 갱신: {}ms", time4);
            log.info(" ├─ [5] 등급 갱신: {}ms", time5);
            log.info(" └─ [6] 쿠폰 발급: {}ms", time6);
        } catch (Exception e) {
            flagManager.endGlobalBadgeBatch();
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/badges")
    public void runBadge(@RequestParam String targetDate,
                         @RequestParam(defaultValue = "100") int batchSize) {
        if (flagManager.isBadgeBatchRunning()) {
            log.warn("[배지 배치 중복 실행 차단] 현재 전역 배치가 진행 중입니다.");
            return;
        }

        flagManager.startGlobalBadgeBatch();
        List<User> users;
        Map<Long, Map<Long, OrderCountAndAmount>> statMap;
        try {
            users = jpaMembershipService.getAllUsers();
            statMap = jpaOrderService.aggregateUserCategoryStats(LocalDate.parse(targetDate));

            List<String> keysToFlag = jpaBadgeService.detectBadgeUpdateTargets(users, statMap);

            runParallelBadgeBatch(keysToFlag, batchSize);
        } finally {
            flagManager.endGlobalBadgeBatch();
        }
    }





    @PostMapping("/users")
    public void runUserLevel(@RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        runParallelUserLevelBatch(users, batchSize);
    }

    @PostMapping("/coupons")
    public void runCoupon(@RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        runParallelCouponBatch(users, batchSize);
    }




    private void runParallelBadgeBatch(List<String> keysToUpdate, int batchSize) {
        Instant totalStart = Instant.now();

        // 1. 🔐 플래그 일괄 등록
        for (String key : keysToUpdate) {
            String[] parts = key.split(":");
            Long userId = Long.parseLong(parts[0]);
            Long categoryId = Long.parseLong(parts[1]);
            flagManager.addBadgeFlag(userId, categoryId);
        }

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<List<String>> partitions = PartitionUtils.partition(keysToUpdate, 6);
        List<Future<?>> futures = new ArrayList<>();

        for (List<String> part : partitions) {
            futures.add(executor.submit(() -> {
                for (int start = 0; start < part.size(); start += 1000) {
                    int end = Math.min(start + 1000, part.size());
                    List<String> chunk = part.subList(start, end);
                    if (chunk.isEmpty()) continue;

                    jpaBadgeService.bulkUpdateBadgeStates(chunk, batchSize);
                }
                return null;
            }));
        }

        try {
            for (Future<?> f : futures) f.get();
        } catch (Exception e) {
            executor.shutdownNow();
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();

            // 2. 🔓 플래그 일괄 해제
            for (String key : keysToUpdate) {
                String[] parts = key.split(":");
                Long userId = Long.parseLong(parts[0]);
                Long categoryId = Long.parseLong(parts[1]);
                flagManager.removeBadgeFlag(userId, categoryId);
            }
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
            futures.add(executor.submit(() -> {
                for (int start = 0; start < part.size(); start += 1000) {
                    int end = Math.min(start + 1000, part.size());
                    List<User> chunk = part.subList(start, end);
                    if (chunk.isEmpty()) continue;
                    Instant t = Instant.now();
                    log.info("[등급 갱신 시작] 유저 {}명", chunk.size());
                    jpaMembershipService.bulkUpdateMembershipLevelsAndLog(chunk, activeBadgeMap, batchSize);
                    long time = Duration.between(t, Instant.now()).toMillis();
                    log.info("[등급 갱신 완료] {}ms", time);
                }
                return null;
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
            futures.add(executor.submit(() -> {
                for (int start = 0; start < part.size(); start += 1000) {
                    int end = Math.min(start + 1000, part.size());
                    List<User> chunk = part.subList(start, end);
                    if (chunk.isEmpty()) continue;
                    Instant t = Instant.now();
                    log.info("[쿠폰 발급 시작] 유저 {}명", chunk.size());
                    jpaCouponService.bulkIssueCoupons(chunk, batchSize);
                    long time = Duration.between(t, Instant.now()).toMillis();
                    log.info("[쿠폰 발급 완료] {}ms", time);
                }
                return null;
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
