package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.CouponAmount;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.BadgeRepository;
import org.example.membership.repository.jpa.CouponIssueLogRepository;
import org.example.membership.repository.jpa.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final BadgeRepository badgeRepository;
    private final CouponRepository couponRepository;
    private final CouponIssueLogRepository couponIssueLogRepository;

    @Transactional
    public List<CouponIssueLog> issueCoupons(User user) {
        int qty = switch (user.getMembershipLevel()) {
            case VIP -> 3;
            case GOLD -> 2;
            case SILVER -> 1;
            default -> 0;
        };

        List<CouponIssueLog> issuedLogs = new ArrayList<>();
        if (qty == 0) return issuedLogs;

        List<Badge> badges = badgeRepository.findByUserAndActiveTrue(user);

        for (Badge badge : badges) {
            Coupon coupon = couponRepository
                    .findByCategory(badge.getCategory())
                    .orElse(null);  // autoIssue 조건 제거
            if (coupon == null) continue;

            int already = couponIssueLogRepository.countByUserAndCoupon(user, coupon);

            for (int i = already; i < qty; i++) {
                CouponIssueLog log = new CouponIssueLog();
                log.setUser(user);
                log.setCoupon(coupon);
                log.setMembershipLevel(user.getMembershipLevel());
                log.setIssuedAt(LocalDateTime.now());
                couponIssueLogRepository.save(log);
                issuedLogs.add(log);
            }
        }

        return issuedLogs;
    }

}
