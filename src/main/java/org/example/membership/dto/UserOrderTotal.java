package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UserOrderTotal {
    private Long userId;
    private BigDecimal totalAmount;
}