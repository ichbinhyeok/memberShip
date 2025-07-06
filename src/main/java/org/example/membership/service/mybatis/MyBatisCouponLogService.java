package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.entity.User;
import org.example.membership.entity.Badge;
import org.example.membership.repository.mybatis.CouponIssueLogMapper;
import org.example.membership.repository.mybatis.BadgeMapper;
import org.example.membership.repository.mybatis.CouponMapper;
import org.example.membership.repository.mybatis.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyBatisCouponLogService {

    private final CouponIssueLogMapper couponIssueLogMapper;
    private final BadgeMapper badgeMapper;
    private final CouponMapper couponMapper;
    private final UserMapper userMapper;

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
    public void bulkIssueCouponsForAllUsers() {
        List<User> users = userMapper.findAll();
        bulkIssueCoupons(users);
    }

    @Transactional
    public void bulkIssueCoupons(List<User> users) {
        for (User user : users) {
            int qty = switch (user.getMembershipLevel()) {
                case VIP -> 3;
                case GOLD -> 2;
                case SILVER -> 1;
                default -> 0;
            };
            if (qty == 0) continue;

            List<Badge> badges = badgeMapper.findByUserIdAndActiveTrue(user.getId());
            for (Badge badge : badges) {
                Coupon coupon = couponMapper.findAutoCouponByCategoryId(badge.getCategory().getId());
                if (coupon == null) continue;
                int already = couponIssueLogMapper.countByUserIdAndCouponId(user.getId(), coupon.getId());
                for (int i = already; i < qty; i++) {
                    CouponIssueLog log = new CouponIssueLog();
                    log.setUser(user);
                    log.setCoupon(coupon);
                    log.setMembershipLevel(user.getMembershipLevel());
                    couponIssueLogMapper.insert(log);
                }
            }
        }
    }
}
