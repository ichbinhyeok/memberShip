package org.example.membership.service.pipeline;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.OrderMapper;
import org.example.membership.repository.mybatis.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyBatisRenewalPipelineService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final BadgeService badgeService;
    private final MembershipService membershipService;
    private final MembershipLogService membershipLogService;
    private final CouponService couponService;
    private final CouponLogService couponLogService;

    public record Stats(long count, BigDecimal amount) {}

    private Map<Long, Map<Long, Stats>> collectStats(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);
        var aggregates = orderMapper.aggregateByUserAndCategoryBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        Map<Long, Map<Long, Stats>> statMap = new HashMap<>();
        for (var row : aggregates) {
            statMap.computeIfAbsent(row.getUserId(), k -> new HashMap<>())
                    .put(row.getCategoryId(), new Stats(row.getOrderCount(), row.getTotalAmount()));
        }
        return statMap;
    }

    @Transactional
    public void runBadgeOnly(LocalDate targetDate) {
        Map<Long, Map<Long, Stats>> statMap = collectStats(targetDate);
        List<User> users = userMapper.findAll();
        for (User user : users) {
            badgeService.updateBadgeStatesForUser(user, statMap.get(user.getId()));
        }
    }

    @Transactional
    public void runLevelOnly() {
        List<User> users = userMapper.findAll();
        for (User user : users) {
            membershipService.updateUserLevel(user);
        }
    }

    @Transactional
    public void runLogOnly() {
        List<User> users = userMapper.findAll();
        for (User user : users) {
            membershipLogService.insertMembershipLog(user, user.getMembershipLevel());
        }
    }

    @Transactional
    public void runCouponOnly() {
        List<User> users = userMapper.findAll();
        for (User user : users) {
            couponService.issueCoupons(user);
        }
    }

    @Transactional
    public void runCouponLogOnly() {
        // assumes coupons already issued separately
    }

    @Transactional
    public void runFull(LocalDate targetDate) {
        Map<Long, Map<Long, Stats>> statMap = collectStats(targetDate);
        List<User> users = userMapper.findAll();
        for (User user : users) {
            badgeService.updateBadgeStatesForUser(user, statMap.get(user.getId()));
            var prev = membershipService.updateUserLevel(user);
            membershipLogService.insertMembershipLog(user, prev);
            var coupons = couponService.issueCoupons(user);
            couponLogService.insertCouponLogs(user, coupons);
        }
    }
}
