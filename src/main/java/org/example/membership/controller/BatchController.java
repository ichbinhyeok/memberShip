package org.example.membership.controller;

// BatchController.java - 배치용 컨트롤러 (도메인별 + full)


import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.User;
import org.example.membership.service.jpa.JpaBadgeService;
import org.example.membership.service.jpa.JpaCouponService;
import org.example.membership.service.jpa.JpaMembershipService;
import org.example.membership.service.jpa.JpaOrderService;
import org.example.membership.service.mybatis.MyBatisBadgeService;
import org.example.membership.service.mybatis.MyBatisCouponService;
import org.example.membership.service.mybatis.MyBatisMembershipService;
import org.example.membership.service.mybatis.MyBatisOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/batch")
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    private final JpaOrderService jpaOrderService;
    private final JpaBadgeService jpaBadgeService;
    private final JpaMembershipService jpaMembershipService;
    private final JpaCouponService jpaCouponService;

    // MyBatis 서비스
    private final MyBatisOrderService myBatisOrderService;
    private final MyBatisBadgeService myBatisBadgeService;
    private final MyBatisMembershipService myBatisMembershipService;
    private final MyBatisCouponService myBatisCouponService;

    @PostMapping("jpa/full")
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
        runBadgeBatch(users, statMap, batchSize);
        long time3 = Duration.between(t3, Instant.now()).toMillis();

        // 4. 등급 갱신
        Instant t4 = Instant.now();
        runUserLevelBatch(users, batchSize);
        long time4 = Duration.between(t4, Instant.now()).toMillis();

        // 5. 쿠폰 발급
        Instant t5 = Instant.now();
        runCouponBatch(users, batchSize);
        long time5 = Duration.between(t5, Instant.now()).toMillis();

        long total = Duration.between(allStart, Instant.now()).toMillis();

        log.info("[전체 배치 완료] 총 소요 시간: {}ms", total);
        log.info(" ├─ [1] 주문 통계 집계: {}ms", time1);
        log.info(" ├─ [2] 유저 조회: {}ms", time2);
        log.info(" ├─ [3] 배지 갱신: {}ms", time3);
        log.info(" ├─ [4] 등급 갱신: {}ms", time4);
        log.info(" └─ [5] 쿠폰 발급: {}ms", time5);
    }


    @PostMapping("jpa/orders")
    public Map<Long, Map<Long, OrderCountAndAmount>> runOrder(@RequestParam String targetDate) {
        return runOrderBatch(LocalDate.parse(targetDate));
    }

    @PostMapping("jpa/badges")
    public void runBadge(@RequestParam String targetDate, @RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = runOrderBatch(LocalDate.parse(targetDate));
        runBadgeBatch(users, statMap, batchSize);
    }

    @PostMapping("jpa/users")
    public void runUserLevel(@RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        runUserLevelBatch(users, batchSize);
    }

    @PostMapping("jpa/coupons")
    public void runCoupon(@RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = jpaMembershipService.getAllUsers();
        runCouponBatch(users, batchSize);
    }



    // === MyBatis 전체 배치 ===

    @PostMapping("/mybatis/full")
    public void runFullMyBatisBatch(@RequestParam String targetDate,
                                    @RequestParam(defaultValue = "100") int batchSize) {
        LocalDate date = LocalDate.parse(targetDate);
        Instant allStart = Instant.now();

        // 1. 주문 통계 집계
        Instant t1 = Instant.now();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = runMyBatisOrderBatch(date);
        long time1 = Duration.between(t1, Instant.now()).toMillis();

        // 2. 유저 전체 조회
        Instant t2 = Instant.now();
        List<User> users = myBatisMembershipService.getAllUsers();
        long time2 = Duration.between(t2, Instant.now()).toMillis();

        // 3. 배지 갱신
        Instant t3 = Instant.now();
        runMyBatisBadgeBatch(users, statMap, batchSize);
        long time3 = Duration.between(t3, Instant.now()).toMillis();

        // 4. 등급 갱신
        Instant t4 = Instant.now();
        runMyBatisUserLevelBatch(users, batchSize);
        long time4 = Duration.between(t4, Instant.now()).toMillis();

        // 5. 쿠폰 발급
        Instant t5 = Instant.now();
        runMyBatisCouponBatch(users, batchSize);
        long time5 = Duration.between(t5, Instant.now()).toMillis();

        long total = Duration.between(allStart, Instant.now()).toMillis();

        log.info("[MyBatis 전체 배치 완료] 총 소요 시간: {}ms", total);
        log.info(" ├─ [1] 주문 통계 집계: {}ms", time1);
        log.info(" ├─ [2] 유저 조회: {}ms", time2);
        log.info(" ├─ [3] 배지 갱신: {}ms", time3);
        log.info(" ├─ [4] 등급 갱신: {}ms", time4);
        log.info(" └─ [5] 쿠폰 발급: {}ms", time5);
    }

    @PostMapping("/mybatis/orders")
    public Map<Long, Map<Long, OrderCountAndAmount>> runMyBatisOrder(@RequestParam String targetDate) {
        return runMyBatisOrderBatch(LocalDate.parse(targetDate));
    }

    @PostMapping("/mybatis/badges")
    public void runMyBatisBadge(@RequestParam String targetDate, @RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = myBatisMembershipService.getAllUsers();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = runMyBatisOrderBatch(LocalDate.parse(targetDate));
        runMyBatisBadgeBatch(users, statMap, batchSize);
    }

    @PostMapping("/mybatis/users")
    public void runMyBatisUserLevel(@RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = myBatisMembershipService.getAllUsers();
        runMyBatisUserLevelBatch(users, batchSize);
    }

    @PostMapping("/mybatis/coupons")
    public void runMyBatisCoupon(@RequestParam(defaultValue = "100") int batchSize) {
        List<User> users = myBatisMembershipService.getAllUsers();
        runMyBatisCouponBatch(users, batchSize);
    }

    // === 내부 로직 ===

    private Map<Long, Map<Long, OrderCountAndAmount>> runMyBatisOrderBatch(LocalDate targetDate) {
        Instant start = Instant.now();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = myBatisOrderService.aggregateUserCategoryStats(targetDate);
        log.info("[MyBatis 1] 주문 통계 집계 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
        return statMap;
    }

    private void runMyBatisBadgeBatch(List<User> users, Map<Long, Map<Long, OrderCountAndAmount>> statMap, int batchSize) {
        Instant start = Instant.now();
        myBatisBadgeService.bulkUpdateBadgeStates(users, statMap, batchSize);
        log.info("[MyBatis 2] 배지 갱신 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
    }

    private void runMyBatisUserLevelBatch(List<User> users, int batchSize) {
        Instant start = Instant.now();
        Map<Long, Long> activeBadgeMap = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> myBatisBadgeService.countActiveBadgesByUserId(user.getId())
                ));
        myBatisMembershipService.bulkUpdateMembershipLevelsAndLog(users, activeBadgeMap, batchSize);
        log.info("[MyBatis 3] 사용자 등급 갱신 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
    }

    private void runMyBatisCouponBatch(List<User> users, int batchSize) {
        Instant start = Instant.now();

        myBatisCouponService.bulkIssueCouponsWithResolvedData(users, batchSize);

        log.info("[MyBatis 4] 쿠폰 발급 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
    }





    // 내부 헬퍼 메소드들 (시간 측정 포함)

    private Map<Long, Map<Long, OrderCountAndAmount>> runOrderBatch(LocalDate targetDate) {
        Instant start = Instant.now();
        Map<Long, Map<Long, OrderCountAndAmount>> statMap = jpaOrderService.aggregateUserCategoryStats(targetDate);
        log.info("[1] 주문 통계 집계 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
        return statMap;
    }

    private void runBadgeBatch(List<User> users, Map<Long, Map<Long, OrderCountAndAmount>> statMap, int batchSize) {
        Instant start = Instant.now();
        jpaBadgeService.bulkUpdateBadgeStates(users, statMap, batchSize);
        log.info("[2] 배지 갱신 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
    }

    private void runUserLevelBatch(List<User> users, int batchSize) {
        Instant start = Instant.now();
        Map<Long, Long> activeBadgeMap = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> (long)jpaMembershipService.getUserStatus(u.getId()).getBadges().size()
                ));
        jpaMembershipService.bulkUpdateMembershipLevelsAndLog(users, activeBadgeMap, batchSize);
        log.info("[3] 사용자 등급 갱신 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
    }

    private void runCouponBatch(List<User> users, int batchSize) {
        Instant start = Instant.now();
        jpaCouponService.bulkIssueCoupons(users, batchSize);
        log.info("[4] 쿠폰 발급 완료: {}ms", Duration.between(start, Instant.now()).toMillis());
    }
}
