package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private int quantity;
    private java.math.BigDecimal itemPrice;
}