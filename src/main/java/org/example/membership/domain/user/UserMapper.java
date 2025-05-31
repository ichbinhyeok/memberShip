package org.example.membership.domain.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.dto.MembershipInfoResponse; // MembershipInfoResponse DTO 임포트
import org.example.membership.common.enums.MembershipLevel;

import java.util.List;

@Mapper
public interface UserMapper {

    // ID로 사용자 조회 (resultMap 사용)
    User selectById(@Param("id") Long id);

    // ID로 멤버십 레벨 조회 (Enum 타입 반환)
    MembershipLevel selectMembershipLevel(@Param("id") Long id);

    // 모든 사용자 조회 (resultMap 사용)
    List<User> findAll();

    // DTO를 사용하여 특정 사용자 멤버십 정보 조회
    MembershipInfoResponse selectMemberShipInfo(@Param("id") Long id);

    void insert(User user);
    User findById(@Param("id") Long id);
    void update(User user);
    void deleteById(@Param("id") Long id);
    List<User> findByMembershipLevel(@Param("level") MembershipLevel level);
}