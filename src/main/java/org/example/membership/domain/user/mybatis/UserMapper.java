package org.example.membership.domain.user.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.domain.user.User;
import org.example.membership.dto.MembershipInfoResponse;
import org.example.membership.dto.UserOrderTotal;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    void insert( User user); // ✅ XML의 <insert id="insert">

    User findById(@Param("id") Long id);   // ✅ <select id="findById">

    List<User> findAll();                  // ✅ <select id="findAll">

    MembershipInfoResponse selectMemberShipInfo(@Param("id") Long id); // ✅ <select id="selectMemberShipInfo">

    void update(User user); // ✅ <update id="update">

    void deleteById(@Param("id") Long id); // ✅ <delete id="deleteById">

    List<User> findByMembershipLevel(@Param("level") MembershipLevel level); // ✅ <select id="findByMembershipLevel">

    MembershipInfoResponse selectMemberShipInfoByName(@Param("userName")  String username);

    List<UserOrderTotal> userOrderAmountBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1);
}