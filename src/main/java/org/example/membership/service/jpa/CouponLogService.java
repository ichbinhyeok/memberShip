package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.entity.User;
import org.example.membership.repository.jpa.CouponIssueLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponLogService {

    private final CouponIssueLogRepository couponIssueLogRepository;

    @Transactional
    public void insertCouponLogs(User user, List<Coupon> coupons) {
        for (Coupon coupon : coupons) {
            CouponIssueLog log = new CouponIssueLog();
            log.setUser(user);
            log.setCoupon(coupon);
            log.setMembershipLevel(user.getMembershipLevel());
            couponIssueLogRepository.save(log);
        }
    }
}
