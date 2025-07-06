package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.OrderStatus;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.entity.Order;
import org.example.membership.repository.jpa.OrderRepository;
import org.example.membership.dto.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JpaOrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUser_Id(userId);

        return orders.stream().map(order -> {
            OrderResponse dto = new OrderResponse();
            dto.setId(order.getId());
            dto.setUserId(order.getUser().getId());
            dto.setOrderAmount(order.getOrderAmount());
            dto.setStatus(order.getStatus());
            dto.setOrderedAt(order.getOrderedAt());
            return dto;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Map<Long, OrderCountAndAmount>> aggregateUserCategoryStats(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> aggregates = orderRepository.aggregateByUserAndCategoryBetween(startDateTime, endDateTime);

        Map<Long, Map<Long, OrderCountAndAmount>> statMap = new HashMap<>();

        for (Object[] row : aggregates) {
            Long userId = (Long) row[0];
            Long categoryId = (Long) row[1];
            Long count = ((Number) row[2]).longValue();
            BigDecimal amount = (BigDecimal) row[3];

            Map<Long, OrderCountAndAmount> categoryMap = statMap.computeIfAbsent(userId, k -> new HashMap<>());
            categoryMap.put(categoryId, new OrderCountAndAmount(count, amount));
        }

        return statMap;
    }

} 