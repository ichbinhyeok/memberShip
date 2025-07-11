package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderWithCouponRequest {
    private Long userId;
    private List<OrderItemRequest> items;
    private String couponIssueId;
}
