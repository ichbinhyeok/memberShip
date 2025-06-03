package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class OrderRequest {
    private Long id;
    private Long userId;
    private BigDecimal orderAmount;
    private OrderStatus status = OrderStatus.PAID;        // 직접 지정
    private LocalDateTime orderedAt = LocalDateTime.now();
} 