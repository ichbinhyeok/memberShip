package org.example.membership.repository.jpa;

import org.example.membership.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    boolean existsByCouponIssueLog_Id(java.util.UUID couponIssueLogId);


}


