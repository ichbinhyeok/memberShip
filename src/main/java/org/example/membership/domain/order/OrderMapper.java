package org.example.membership.domain.order;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.common.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    void insert(Order order);
    Order findById(@Param("id") Long id);
    List<Order> findAll();
    void update(Order order);
    void deleteById(@Param("id") Long id);
    List<Order> findByStatus(@Param("status") OrderStatus status);
    List<Order> findByUserId(@Param("userId") Long userId);
    
    List<Order> findByOrderedAtBetweenAndStatus(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("status") String status
    );
    
    List<Order> findByUserIdAndOrderedAtBetween(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
} 