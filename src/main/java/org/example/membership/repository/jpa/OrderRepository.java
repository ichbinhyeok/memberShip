package org.example.membership.repository.jpa;


import org.example.membership.common.enums.OrderStatus;
import org.example.membership.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUser_Id(Long userId);


    @Query("SELECT o.user.id, SUM(o.orderAmount - COALESCE(c.discountAmount,0)) " +
            "FROM Order o JOIN o.product p LEFT JOIN o.coupon c " +
            "WHERE o.status = 'PAID' AND o.orderedAt BETWEEN :start AND :end " +
            "GROUP BY o.user.id")
    List<Object[]> sumOrderAmountByUserBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT o.user.id, o.product.category.id, COUNT(o), SUM(o.orderAmount - COALESCE(c.discountAmount,0)) " +
            "FROM Order o LEFT JOIN o.coupon c " +
            "WHERE o.status = 'PAID' AND o.orderedAt BETWEEN :start AND :end " +
            "GROUP BY o.user.id, o.product.category.id")
    List<Object[]> aggregateByUserAndCategoryBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}