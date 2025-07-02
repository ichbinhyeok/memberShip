package org.example.membership.service.pipeline;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.OrderMapper;
import org.example.membership.repository.mybatis.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.membership.dto.UserCategoryStats;
import org.example.membership.service.pipeline.BadgeServiceMyBatis;
import org.example.membership.service.pipeline.MembershipServiceMyBatis;
import org.example.membership.service.pipeline.MembershipLogServiceMyBatis;
import org.example.membership.service.pipeline.CouponServiceMyBatis;
import org.example.membership.service.pipeline.CouponLogServiceMyBatis;
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
    private final BadgeServiceMyBatis badgeService;
    private final MembershipServiceMyBatis membershipService;
    private final MembershipLogServiceMyBatis membershipLogService;
    private final CouponServiceMyBatis couponService;
    private final CouponLogServiceMyBatis couponLogService;


    private Map<Long, Map<Long, UserCategoryStats>> collectStats(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);
        var aggregates = orderMapper.aggregateByUserAndCategoryBetween(
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        Map<Long, Map<Long, UserCategoryStats>> statMap = new HashMap<>();
        for (var row : aggregates) {
            UserCategoryStats stats = new UserCategoryStats();
            stats.setCount(row.getOrderCount());
            stats.setAmount(row.getTotalAmount());
            statMap.computeIfAbsent(row.getUserId(), k -> new HashMap<>())
                    .put(row.getCategoryId(), stats);
        }
        return statMap;
    }

    @Transactional
    public void runBadgeOnly(LocalDate targetDate) {
        Map<Long, Map<Long, UserCategoryStats>> statMap = collectStats(targetDate);
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
        Map<Long, Map<Long, UserCategoryStats>> statMap = collectStats(targetDate);
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
