package org.example.membership.domain.log.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.domain.log.MembershipLog;
import org.example.membership.dto.MembershipLogRequest;

import java.util.List;

@Mapper
public interface MembershipLogMapper {
    void insert(MembershipLog membershipLog);
    void bulkInsertLogs(@Param("list") List<MembershipLog> logs);
    void bulkInsertRequests(@Param("list") List<MembershipLogRequest> logs); // foreach용

    void insertOneRequest(MembershipLogRequest log); // ExecutorType.BATCH용
}


