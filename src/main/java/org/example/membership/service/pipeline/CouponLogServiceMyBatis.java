package org.example.membership.service.pipeline;

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
public class CouponLogServiceMyBatis {

    private final CouponIssueLogMapper couponIssueLogMapper;

    @Transactional
    public void saveAll(List<CouponIssueLog> logs) {
        couponIssueLogMapper.insertAll(logs);
    }

    @Transactional
    public void insertCouponLogs(User user, List<Coupon> coupons) {
        List<CouponIssueLog> logs = new java.util.ArrayList<>();
        for (Coupon coupon : coupons) {
            CouponIssueLog log = new CouponIssueLog();
            log.setUser(user);
            log.setCoupon(coupon);
            log.setMembershipLevel(user.getMembershipLevel());
            log.setIssuedAt(java.time.LocalDateTime.now());
            logs.add(log);
        }
        couponIssueLogMapper.insertAll(logs);
    }
}
