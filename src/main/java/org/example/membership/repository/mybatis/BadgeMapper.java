package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.dto.UserBadgeCount;
import org.example.membership.entity.Badge;
import java.util.Map;
import org.apache.ibatis.annotations.MapKey;


import java.util.List;



@Mapper
public interface BadgeMapper {
    void insert(Badge badge);
    Badge findById(@Param("id") Long id);
    List<Badge> findByUserId(@Param("userId") Long userId);
    List<Badge> findByUserIdAndActiveTrue(@Param("userId") Long userId);
    long countByUserId(@Param("userId") Long userId);
    long countByUserIdAndActiveTrue(@Param("userId") Long userId);
    boolean existsByUserIdAndCategoryId(@Param("userId") Long userId, @Param("categoryId") Long categoryId);
    void update(Badge badge);
    void deleteById(@Param("id") Long id);
    List<Badge> findByUserIds(@Param("list") List<Long> userIds);

    List<UserBadgeCount> countActiveBadgesForUsers(@Param("list") List<Long> userIds);


    void bulkUpdateBadgesCaseWhen(List<Badge> chunk);
}