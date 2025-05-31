package org.example.membership.domain.log;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MembershipLogMapper {
    void insertLog(MembershipLog log);
    List<MembershipLog> findByUserId(@Param("userId") Long userId);
} 