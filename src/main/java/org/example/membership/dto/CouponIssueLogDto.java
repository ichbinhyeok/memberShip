package org.example.membership.dto;


import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CouponIssueLogDto {
    private UUID id;
    private Long userId;
    private Long couponId;
    private MembershipLevel membershipLevel;
    private LocalDateTime issuedAt;


}
