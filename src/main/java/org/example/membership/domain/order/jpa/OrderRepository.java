package org.example.membership.domain.order.jpa;

import org.example.membership.common.enums.OrderStatus;
import org.example.membership.domain.order.Order;
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


    @Query("SELECT o.user.id, SUM(o.orderAmount) " +
            "FROM Order o " +
            "WHERE o.orderedAt BETWEEN :start AND :end " +
            "GROUP BY o.user.id")
    List<Object[]> sumOrderAmountByUserBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}

