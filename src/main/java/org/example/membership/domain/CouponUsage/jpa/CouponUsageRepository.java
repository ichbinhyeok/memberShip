package org.example.membership.domain.CouponUsage.jpa;

import org.example.membership.domain.CouponUsage.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
}
