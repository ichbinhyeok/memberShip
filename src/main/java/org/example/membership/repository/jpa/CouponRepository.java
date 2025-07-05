package org.example.membership.repository.jpa;

import org.example.membership.entity.Category;
import org.example.membership.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCategory(Category category);

}