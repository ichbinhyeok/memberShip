package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.common.enums.OrderStatus;
import org.example.membership.dto.OrderCreateRequest;
import org.example.membership.dto.OrderRequest;
import org.example.membership.dto.OrderResponse;
import org.example.membership.entity.Order;
import org.example.membership.service.jpa.JpaOrderService;
import org.example.membership.config.MyWasInstanceHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final JpaOrderService jpaOrderService;
    private final MyWasInstanceHolder myWasInstanceHolder;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderCreateRequest order) {
        if (!myWasInstanceHolder.isMyUser(order.getUserId())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("이 요청은 현재 WAS 인스턴스에서 처리하지 않습니다.");
        }
        OrderResponse resp = jpaOrderService.createOrder(order);
        return ResponseEntity.ok(resp);
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
