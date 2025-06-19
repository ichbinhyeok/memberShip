package org.example.membership.domain.CouponUsage.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.domain.CouponUsage.CouponUsage;

import java.util.List;

@Mapper
public interface CouponUsageMapper {
    void insert(CouponUsage usage);
    CouponUsage findById(@Param("id") Long id);
    List<CouponUsage> findAll();
    void deleteById(@Param("id") Long id);
}
