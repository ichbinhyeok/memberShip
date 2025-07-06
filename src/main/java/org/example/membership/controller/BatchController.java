package org.example.membership.controller;

// BatchController.java - 배치용 컨트롤러 (도메인별 + full)

package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.User;
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

    @PostMapping("jpa/full")
    public void runFullBatch(@RequestParam String targetDate,
                             @RequestParam(defaultValue = "100") int batchSize) {
        LocalDate date = LocalDate.parse(targetDate);
        Instant allStart = Instant.now();

        Map<Long, Map<Long, OrderCountAndAmount>> statMap = runOrderBatch(date);
        List<User> users = jpaMembershipService.getAllUsers();
        runBadgeBatch(users, statMap, batchSize);
        runUserLevelBatch(users, batchSize);
        runCouponBatch(users, batchSize);

        log.info("[전체 배치 완료] 총 소요 시간: {}ms", Duration.between(allStart, Instant.now()).toMillis());
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
