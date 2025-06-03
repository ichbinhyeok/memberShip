package org.example.membership.domain.order.jpa;

import org.example.membership.common.enums.OrderStatus;
import org.example.membership.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUser_Id(Long userId);
} 