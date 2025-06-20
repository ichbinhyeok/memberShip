package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.Order;
import org.example.membership.repository.mybatis.OrderMapper;
import org.example.membership.common.enums.OrderStatus;
import org.example.membership.dto.OrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
} 