package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.membership.entity.CouponIssueLog;

import java.util.List;
import java.util.Map;

@Mapper
public interface CouponIssueLogMapper {
    void insert(CouponIssueLog log);
    void insertAll(@Param("list") List<CouponIssueLog> logs); //

    CouponIssueLog findById(@Param("id") Long id);
    List<CouponIssueLog> findByUserId(@Param("userId") Long userId);
    List<CouponIssueLog> findAll();
    void deleteById(@Param("id") Long id);
    int countByUserIdAndCouponId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @MapKey("userCouponKey")
    @Select("""
        SELECT CONCAT(user_id, '-', coupon_id) AS userCouponKey, COUNT(*) AS count
        FROM coupon_issue_log
        GROUP BY user_id, coupon_id
    """)
    Map<String, Long> countIssuedPerUserAndCoupon(); // key = userId-couponId, value = 발급 수

}
