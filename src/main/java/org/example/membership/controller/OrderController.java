package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.domain.order.Order;
import org.example.membership.service.jpa.JpaOrderService;
import org.example.membership.service.mybatis.MyBatisOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final JpaOrderService jpaOrderService;
    private final MyBatisOrderService myBatisOrderService;

    @GetMapping("/jpa")
    public ResponseEntity<List<Order>> getAllOrdersJpa() {
        return ResponseEntity.ok(jpaOrderService.getAllOrders());
    }

    @GetMapping("/mybatis")
    public ResponseEntity<List<Order>> getAllOrdersMyBatis() {
        return ResponseEntity.ok(myBatisOrderService.getAllOrders());
    }

    @PostMapping("/jpa")
    public ResponseEntity<Order> createOrderJpa(@RequestBody Order order) {
        return ResponseEntity.ok(jpaOrderService.createOrder(order));
    }

    @PostMapping("/mybatis")
    public ResponseEntity<Order> createOrderMyBatis(@RequestBody Order order) {
        return ResponseEntity.ok(myBatisOrderService.createOrder(order));
    }

    @GetMapping("/jpa/users/{userId}")
    public ResponseEntity<List<Order>> getUserOrdersJpa(@PathVariable Long userId) {
        return ResponseEntity.ok(jpaOrderService.getOrdersByUserId(userId));
    }

    @GetMapping("/mybatis/users/{userId}")
    public ResponseEntity<List<Order>> getUserOrdersMyBatis(@PathVariable Long userId) {
        return ResponseEntity.ok(myBatisOrderService.getOrdersByUserId(userId));
    }
} 