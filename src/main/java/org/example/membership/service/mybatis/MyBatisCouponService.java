package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.*;
import org.example.membership.repository.mybatis.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyBatisCouponService {

    private final CouponIssueLogMapper couponIssueLogMapper;
    private final BadgeMapper badgeMapper;
    private final CouponMapper couponMapper;


    @Transactional
    public void insertCouponLogs(User user, List<Coupon> coupons) {
        for (Coupon coupon : coupons) {
            CouponIssueLog log = new CouponIssueLog();
            log.setUser(user);
            log.setCoupon(coupon);
            log.setMembershipLevel(user.getMembershipLevel());
            couponIssueLogMapper.insert(log);
        }
    }



    @Transactional
    public void bulkIssueCoupons(List<User> users,
                                 Map<Long, List<Badge>> badgeMap,
                                 Map<Long, Coupon> couponMap,
                                 Map<String, Long> issuedCountMap,
                                 int batchSize) {
        int count = 0;

        for (User user : users) {
            int qty = switch (user.getMembershipLevel()) {
                case VIP -> 3;
                case GOLD -> 2;
                case SILVER -> 1;
                default -> 0;
            };
            if (qty == 0) continue;

            List<Badge> badges = badgeMap.getOrDefault(user.getId(), List.of());

            for (Badge badge : badges) {
                Coupon coupon = couponMap.get(badge.getCategory().getId());
                if (coupon == null) continue;

                String key = user.getId() + "-" + coupon.getId();
                long already = issuedCountMap.getOrDefault(key, 0L);

                for (int i = (int) already; i < qty; i++) {
                    CouponIssueLog log = new CouponIssueLog();
                    log.setUser(user);
                    log.setCoupon(coupon);
                    log.setMembershipLevel(user.getMembershipLevel());
                    log.setIssuedAt(LocalDateTime.now());

                    couponIssueLogMapper.insert(log); // MyBatis는 명시적 insert 필요

                    count++;
                    if (count % batchSize == 0) {
                        System.out.println("Flushed " + count + " coupon logs");
                    }
                }
            }
        }
    }


    @Transactional
    public void bulkIssueCouponsWithResolvedData(List<User> users, int batchSize) {
        List<Long> userIds = users.stream().map(User::getId).toList();

        List<Badge> allBadges = badgeMapper.findByUserIds(userIds);
        Map<Long, List<Badge>> badgeMap = allBadges.stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));

        Map<Long, Coupon> couponMap = couponMapper.findAll().stream()
                .collect(Collectors.toMap(c -> c.getCategory().getId(), c -> c));

        Map<String, Long> issuedCountMap = couponIssueLogMapper.countIssuedPerUserAndCoupon();

        bulkIssueCoupons(users, badgeMap, couponMap, issuedCountMap, batchSize);
    }

    @Transactional
    public void issueCouponsForUsers(List<User> users, int batchSize) {
        List<Long> userIds = users.stream().map(User::getId).toList();

        List<Badge> allBadges = badgeMapper.findByUserIds(userIds);
        Map<Long, List<Badge>> badgeMap = allBadges.stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getId()));

        Map<Long, Coupon> couponMap = couponMapper.findAll().stream()
                .collect(Collectors.toMap(c -> c.getCategory().getId(), c -> c));

        Map<String, Long> issuedCountMap = couponIssueLogMapper.countIssuedPerUserAndCoupon();

        bulkIssueCoupons(users, badgeMap, couponMap, issuedCountMap, batchSize);
    }

}
