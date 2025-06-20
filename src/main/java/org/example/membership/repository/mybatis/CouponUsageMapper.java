package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.entity.CouponUsage;

import java.util.List;

@Mapper
public interface CouponUsageMapper {
    void insert(CouponUsage usage);
    CouponUsage findById(@Param("id") Long id);
    List<CouponUsage> findAll();
    void deleteById(@Param("id") Long id);
}
