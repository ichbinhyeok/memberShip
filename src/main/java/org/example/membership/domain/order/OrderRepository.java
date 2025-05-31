package org.example.membership.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.example.membership.common.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrderedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, OrderStatus status);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByUserId(Long userId);
} 