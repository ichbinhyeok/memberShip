package org.example.membership.domain.log.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.domain.log.MembershipLog;

import java.util.List;

@Mapper
public interface MembershipLogMapper {
    void insert(MembershipLog membershipLog);
    void bulkInsertLogs(@Param("list") List<MembershipLog> logs);


}


