package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.dto.UserCategoryOrderStats;
import org.example.membership.entity.Order;
import org.example.membership.repository.mybatis.OrderMapper;
import org.example.membership.common.enums.OrderStatus;
import org.example.membership.dto.OrderRequest;
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
public class MyBatisOrderService {
    private final OrderMapper orderMapper;

    @Transactional
    public OrderRequest createOrder(OrderRequest order) {
        order.setOrderedAt(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderMapper.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderMapper.findAll();
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        orderMapper.update(order);
        return order;
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderMapper.deleteById(id);
    }


    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Map<Long, OrderCountAndAmount>> aggregateUserCategoryStats(LocalDate targetDate) {
        LocalDate startDate = targetDate.withDayOfMonth(1).minusMonths(3);
        LocalDate endDate = targetDate.withDayOfMonth(1).minusDays(1);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<UserCategoryOrderStats> aggregates = orderMapper.aggregateByUserAndCategoryBetween(startDateTime, endDateTime);

        Map<Long, Map<Long, OrderCountAndAmount>> statMap = new HashMap<>();

        for (UserCategoryOrderStats stat : aggregates) {
            Long userId = stat.getUserId();
            Long categoryId = stat.getCategoryId();
            Long count = stat.getOrderCount();
            BigDecimal amount = stat.getTotalAmount();

            Map<Long, OrderCountAndAmount> categoryMap = statMap.computeIfAbsent(userId, k -> new HashMap<>());
            categoryMap.put(categoryId, new OrderCountAndAmount(count, amount));
        }

        return statMap;
    }



}

