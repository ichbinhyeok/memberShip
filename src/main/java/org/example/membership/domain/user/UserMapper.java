package org.example.membership.domain.user;

import org.apache.ibatis.annotations.Mapper;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.dto.MembershipInfoResponse;

import java.util.List;

@Mapper
public interface UserMapper {
    void insert(User user);
    User findById(Long id);
    List<User> findAll();
    void update(User user);
    void deleteById(Long id);
    List<User> findByMembershipLevel(MembershipLevel level);
    User findByUsername(String username);
    MembershipInfoResponse selectMemberShipInfo(Long id);
} 