package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderCreateRequest {
    private Long userId;
    private List<OrderItemRequest> items;
    private String couponIssueId; // optional
}
