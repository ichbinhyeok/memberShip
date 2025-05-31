package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderRequest {
    private Long userId;
    private BigDecimal orderAmount;
} 