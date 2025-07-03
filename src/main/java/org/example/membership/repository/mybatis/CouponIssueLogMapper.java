package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.entity.CouponIssueLog;

import java.util.List;

@Mapper
public interface CouponIssueLogMapper {
    void insert(CouponIssueLog log);
    CouponIssueLog findById(@Param("id") Long id);
    List<CouponIssueLog> findByUserId(@Param("userId") Long userId);
    List<CouponIssueLog> findAll();
    void deleteById(@Param("id") Long id);
    int countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);
}