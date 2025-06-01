package org.example.membership.domain.order.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.domain.order.Order;

import java.util.List;

@Mapper
public interface OrderMapper {

    void insert(@Param("order") Order order); //
    Order findById(@Param("id") Long id); //
    List<Order> findByUserId(@Param("userId") Long userId); //
    List<Order> findAll(); //
    void update(@Param("order") Order order); //
    void deleteById(@Param("id") Long id); //
}
