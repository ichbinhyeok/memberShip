package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.OrderStatus;
import org.example.membership.dto.OrderResponse;
import org.example.membership.entity.Order;
import org.example.membership.service.jpa.JpaOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final JpaOrderService jpaOrderService;

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return jpaOrderService.createOrder(order);
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return jpaOrderService.getOrderById(id);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return jpaOrderService.getAllOrders();
    }

    @PatchMapping("/{id}/status")
    public Order updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return jpaOrderService.updateOrderStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        jpaOrderService.deleteOrder(id);
    }

    @GetMapping("/user/{userId}")
    public List<OrderResponse> getOrdersByUserId(@PathVariable Long userId) {
        return jpaOrderService.getOrdersByUserId(userId);
    }
}
