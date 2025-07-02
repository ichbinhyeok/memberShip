package org.example.membership.service.pipeline;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.CouponAmount;
import org.example.membership.entity.Badge;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.BadgeMapper;
import org.example.membership.repository.mybatis.CouponMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponServiceMyBatis {

    private final CouponMapper couponMapper;
    private final BadgeMapper badgeMapper;

    @Transactional
    public void createCoupons(List<Coupon> coupons) {
        for (Coupon coupon : coupons) {
            couponMapper.insert(coupon);
        }
    }

    @Transactional
    public List<Coupon> issueCoupons(User user) {
        int qty = switch (user.getMembershipLevel()) {
            case VIP -> 3;
            case GOLD -> 2;
            case SILVER -> 1;
            default -> 0;
        };
        List<Coupon> issued = new ArrayList<>();
        if (qty == 0) return issued;
        List<Badge> badges = badgeMapper.findByUserId(user.getId());
        for (Badge badge : badges) {
            if (!badge.isActive()) continue;
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
}
