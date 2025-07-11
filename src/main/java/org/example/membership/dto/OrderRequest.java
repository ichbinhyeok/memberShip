package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderRequest {
    private Long id;
    private Long userId;
    private Long couponId;
    private BigDecimal totalAmount;
    private List<OrderItemRequest> items;
    private OrderStatus status = OrderStatus.PAID;        // 직접 지정
    private LocalDateTime orderedAt = LocalDateTime.now();
    private UUID couponIssueId;


}