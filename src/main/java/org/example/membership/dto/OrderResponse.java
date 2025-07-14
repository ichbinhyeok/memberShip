package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
public class OrderResponse {
    private Long id;
    private Long userId;
    private Long couponId;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private OrderStatus status;
    private LocalDateTime orderedAt;

    private BigDecimal originalAmount;   // 주문 총액 (할인 전)
    private BigDecimal discountAmount;   // 할인 금액
    private BigDecimal finalAmount;      // 최종 결제 금액
}