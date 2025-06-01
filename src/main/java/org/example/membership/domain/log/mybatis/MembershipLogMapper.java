package org.example.membership.domain.log.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.domain.log.MembershipLog;

@Mapper
public interface MembershipLogMapper {
    void save(@Param("membershipLog") MembershipLog membershipLog);
}
