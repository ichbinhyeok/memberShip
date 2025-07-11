package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.dto.OrderCountAndAmount;
import org.example.membership.dto.OrderItemRequest;
import org.example.membership.dto.UserCategoryOrderStats;
import org.example.membership.entity.Order;
import org.example.membership.entity.OrderItem;
import org.example.membership.entity.Product;
import org.example.membership.repository.mybatis.OrderItemMapper;
import org.example.membership.repository.mybatis.OrderMapper;
import org.example.membership.common.enums.OrderStatus;
import org.example.membership.dto.OrderRequest;
import org.example.membership.repository.mybatis.ProductMapper;
import org.example.membership.repository.mybatis.UserMapper;
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
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    @Transactional
    public OrderRequest createOrder(OrderRequest order) {
        order.setOrderedAt(LocalDateTime.now());
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        if (order.getItems() != null) {
            for (OrderItemRequest itemReq : order.getItems()) {
                Product product = productMapper.findById(itemReq.getProductId());
                total = total.add(product.getPrice().multiply(java.math.BigDecimal.valueOf(itemReq.getQuantity())));
            }
        }
        order.setTotalAmount(total);
        orderMapper.insert(order);
        if (order.getItems() != null) {
            for (org.example.membership.dto.OrderItemRequest itemReq : order.getItems()) {
                OrderItem item = new OrderItem();
                org.example.membership.entity.Order ref = new org.example.membership.entity.Order();
                ref.setId(order.getId());
                item.setOrder(ref);
                org.example.membership.entity.Product product = productMapper.findById(itemReq.getProductId());
                item.setProduct(product);
                item.setQuantity(itemReq.getQuantity());
                item.setItemPrice(product.getPrice());
                orderItemMapper.insert(item);
            }
        }
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

