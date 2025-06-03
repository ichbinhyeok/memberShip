package org.example.membership.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.membership.domain.order.Order;
import org.example.membership.domain.order.jpa.OrderRepository;
import org.example.membership.domain.user.jpa.UserRepository;
import org.example.membership.dto.OrderRequest;
import org.example.membership.dto.OrderResponse;
import org.example.membership.service.jpa.JpaOrderService;
import org.example.membership.service.mybatis.MyBatisOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "주문 관리", description = "주문 관련 API")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final JpaOrderService jpaOrderService;
    private final MyBatisOrderService myBatisOrderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Operation(summary = "JPA로 전체 주문 조회", description = "JPA를 사용하여 모든 주문 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/jpa")
    public ResponseEntity<List<OrderResponse>> getAllOrdersJpa() {
        List<Order> orders = jpaOrderService.getAllOrders();

        List<OrderResponse> response = orders.stream().map(order -> {
            OrderResponse dto = new OrderResponse();
            dto.setId(order.getId());
            dto.setUserId(order.getUser().getId()); // LAZY 로딩 여기서 터짐 (문제없음)
            dto.setOrderAmount(order.getOrderAmount());
            dto.setStatus(order.getStatus());
            dto.setOrderedAt(order.getOrderedAt());
            return dto;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "MyBatis로 전체 주문 조회", description = "MyBatis를 사용하여 모든 주문 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/mybatis")
    public ResponseEntity<List<Order>> getAllOrdersMyBatis() {
        return ResponseEntity.ok(myBatisOrderService.getAllOrders());
    }

    @Operation(summary = "JPA로 주문 생성", description = "JPA를 사용하여 새로운 주문을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/jpa")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(request.getUserId()));
        order.setOrderAmount(request.getOrderAmount());
        order.setStatus(request.getStatus());
        order.setOrderedAt(request.getOrderedAt());

        Order saved = orderRepository.save(order);

        OrderResponse response = new OrderResponse();
        response.setId(saved.getId());
        response.setUserId(saved.getUser().getId());
        response.setOrderAmount(saved.getOrderAmount());
        response.setStatus(saved.getStatus());
        response.setOrderedAt(saved.getOrderedAt());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "MyBatis로 주문 생성", description = "MyBatis를 사용하여 새로운 주문을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/mybatis")
    public ResponseEntity<OrderRequest> createOrderMyBatis(
            @Parameter(description = "주문 정보", required = true) @RequestBody OrderRequest order
    ) {
        return ResponseEntity.ok(myBatisOrderService.createOrder(order));
    }

    @Operation(summary = "JPA로 사용자별 주문 조회", description = "JPA를 사용하여 특정 사용자의 주문 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/jpa/users/{userId}")
    public ResponseEntity<List<OrderResponse>> getUserOrdersJpa(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId
    ) {
        return ResponseEntity.ok(jpaOrderService.getOrdersByUserId(userId));
    }

    @Operation(summary = "MyBatis로 사용자별 주문 조회", description = "MyBatis를 사용하여 특정 사용자의 주문 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/mybatis/users/{userId}")
    public ResponseEntity<List<Order>> getUserOrdersMyBatis(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId
    ) {
        return ResponseEntity.ok(myBatisOrderService.getOrdersByUserId(userId));
    }
} 