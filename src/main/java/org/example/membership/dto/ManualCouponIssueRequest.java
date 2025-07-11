package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualCouponIssueRequest {
    private Long userId;
    private String couponCode;
}
