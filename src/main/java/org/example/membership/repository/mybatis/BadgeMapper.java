package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.entity.Badge;

import java.util.List;

@Mapper
public interface BadgeMapper {
    List<Badge> findByUserId(@Param("userId") Long userId);
    List<Badge> findByUserIdAndActiveTrue(@Param("userId") Long userId);
    long countByUserId(@Param("userId") Long userId);
    long countByUserIdAndActiveTrue(@Param("userId") Long userId);
    void update(Badge badge);
}
