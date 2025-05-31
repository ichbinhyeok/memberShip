package org.example.membership.domain.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    List<User> findAll();
    User findById(@Param("id") Long id);
    void updateMembershipLevel(@Param("id") Long id, @Param("level") String level);
    List<User> findUsersByMembershipLevel(@Param("level") String level);
} 