package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CouponInfoResponse {
    private Long id;
    private String code;
    private String category;
    private LocalDateTime expiresAt;
}
