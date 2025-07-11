package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.dto.OrderRequest;
import org.example.membership.dto.UserCategoryOrderStats;
import org.example.membership.dto.UserOrderTotal;
import org.example.membership.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.entity.OrderItem;

import java.util.List;

@Mapper
public interface OrderItemMapper {
    void insert(OrderItem item);

    OrderItem findById(@Param("id") Long id);

    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    void update(@Param("item") OrderItem item);

    void deleteById(@Param("id") Long id);
}