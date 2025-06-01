package org.example.membership.service.mybatis;

import lombok.RequiredArgsConstructor;
import org.example.membership.domain.order.Order;
import org.example.membership.domain.order.mybatis.OrderMapper;
import org.example.membership.common.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyBatisOrderService {
    private final OrderMapper orderMapper;

    @Transactional
    public Order createOrder(Order order) {
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