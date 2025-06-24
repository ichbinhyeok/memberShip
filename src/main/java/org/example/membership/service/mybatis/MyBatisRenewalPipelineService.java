package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.BadgeMapper;
import org.example.membership.repository.mybatis.CouponMapper;
import org.example.membership.repository.mybatis.MembershipLogMapper;
import org.example.membership.repository.mybatis.OrderMapper;
import org.example.membership.repository.mybatis.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.membership.service.mybatis.MyBatisBadgeService;
import org.example.membership.service.mybatis.MyBatisBadgeService.Stats;
import org.example.membership.service.mybatis.MyBatisCouponLogService;
import org.example.membership.common.enums.CouponAmount;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.MembershipLog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MyBatisRenewalPipelineService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final MembershipLogMapper membershipLogMapper;
    private final CouponMapper couponMapper;
    private final BadgeMapper badgeMapper;
    private final MyBatisBadgeService badgeService;
    private final MyBatisCouponLogService couponLogService;


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
            updateMembershipLevel(user);
        }
    }

    @Transactional
    public void runLogOnly() {
        List<User> users = userMapper.findAll();
        for (User user : users) {
            insertMembershipLog(user, user.getMembershipLevel());
        }
    }

    @Transactional
    public void runCouponOnly() {
        List<User> users = userMapper.findAll();
        for (User user : users) {
            issueCoupons(user);
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
            var prev = updateMembershipLevel(user);
            insertMembershipLog(user, prev);
            var coupons = issueCoupons(user);
            couponLogService.insertCouponLogs(user, coupons);
        }
    }

    private MembershipLevel updateMembershipLevel(User user) {
        long activeCount = badgeMapper.countByUserIdAndActiveTrue(user.getId());
        MembershipLevel previous = user.getMembershipLevel();
        MembershipLevel newLevel = calculateLevel(activeCount);
        user.setMembershipLevel(newLevel);
        user.setLastMembershipChange(LocalDateTime.now());
        userMapper.update(user);
        return previous;
    }

    private void insertMembershipLog(User user, MembershipLevel previous) {
        var log = new MembershipLog();
        log.setUser(user);
        log.setPreviousLevel(previous);
        log.setNewLevel(user.getMembershipLevel());
        log.setChangeReason("badge count: " + user.getMembershipLevel());
        log.setChangedAt(LocalDateTime.now());
        membershipLogMapper.insert(log);
    }

    private List<Coupon> issueCoupons(User user) {
        int qty = switch (user.getMembershipLevel()) {
            case VIP -> 3;
            case GOLD -> 2;
            case SILVER -> 1;
            default -> 0;
        };
        List<Coupon> issued = new ArrayList<>();
        if (qty == 0) return issued;
        List<Badge> badges = badgeMapper.findByUserIdAndActiveTrue(user.getId());
        for (Badge badge : badges) {
            for (int i = 0; i < qty; i++) {
                Coupon c = new Coupon();
                c.setCode("AUTO-" + UUID.randomUUID().toString().substring(0, 8));
                c.setDiscountAmount(CouponAmount.W1000);
                c.setCategory(badge.getCategory());
                couponMapper.insert(c);
                issued.add(c);
            }
        }
        return issued;
    }

    private MembershipLevel calculateLevel(long badgeCount) {
        if (badgeCount >= 3) {
            return MembershipLevel.VIP;
        } else if (badgeCount == 2) {
            return MembershipLevel.GOLD;
        } else if (badgeCount == 1) {
            return MembershipLevel.SILVER;
        }
        return MembershipLevel.NONE;
    }
}
