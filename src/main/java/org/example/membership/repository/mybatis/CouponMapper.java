package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.entity.Coupon;

import java.util.List;

@Mapper
public interface CouponMapper {
    void insert(Coupon coupon);
    Coupon findById(@Param("id") Long id);
    List<Coupon> findAll();
    void update(Coupon coupon);
    void deleteById(@Param("id") Long id);
    Coupon findAutoCouponByCategoryId(@Param("categoryId") Long categoryId);
}