package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class OrderResponse {
    private Long id;
    private Long userId;
    private BigDecimal orderAmount;
    private OrderStatus status;
    private LocalDateTime orderedAt;
} 