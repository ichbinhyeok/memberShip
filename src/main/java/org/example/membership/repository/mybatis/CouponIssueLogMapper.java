package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.entity.CouponIssueLog;

import java.util.List;

@Mapper
public interface CouponIssueLogMapper {
    void insertAll(@Param("list") List<CouponIssueLog> logs);
}
