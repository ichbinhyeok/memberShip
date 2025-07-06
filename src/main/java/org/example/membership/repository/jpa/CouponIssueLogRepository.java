package org.example.membership.repository.jpa;

import org.example.membership.entity.Coupon;
import org.example.membership.entity.CouponIssueLog;
import org.example.membership.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponIssueLogRepository extends JpaRepository<CouponIssueLog, Long> {
    List<CouponIssueLog> findByUser(User user);
    int countByUserAndCoupon(User user, Coupon coupon);

    List<CouponIssueLog> findAllByUserIn(List<User> users);
}
