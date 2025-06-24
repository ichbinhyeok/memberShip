package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.Coupon;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.entity.User;
import org.example.membership.repository.mybatis.CouponIssueLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyBatisCouponLogService {

    private final CouponIssueLogMapper couponIssueLogMapper;

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
}
