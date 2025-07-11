package org.example.membership.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.dto.OrderRequest;
import org.example.membership.dto.UserOrderTotal;
import org.example.membership.dto.UserCategoryOrderStats;
import org.example.membership.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    void insert(OrderRequest order);

    List<Order> findWithItemsByUserId(@Param("userId") Long userId);

    Order findById(@Param("id") Long id);

    List<Order> findByUserId(@Param("userId") Long userId);

    List<Order> findAll();

    void update(@Param("order") Order order);

    void deleteById(@Param("id") Long id);

    List<UserOrderTotal> sumOrderAmountByUserBetween(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    List<UserCategoryOrderStats> aggregateByUserAndCategoryBetween(@Param("start") LocalDateTime start,
                                                                   @Param("end") LocalDateTime end);
}