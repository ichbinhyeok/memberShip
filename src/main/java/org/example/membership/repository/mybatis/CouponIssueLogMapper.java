package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.example.membership.entity.CouponIssueLog;

@Mapper
public interface CouponIssueLogMapper {
    void insert(CouponIssueLog log);
}
