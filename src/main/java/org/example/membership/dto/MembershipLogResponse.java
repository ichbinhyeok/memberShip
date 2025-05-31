package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;

import java.time.LocalDateTime;

@Getter
@Setter
public class MembershipLogResponse {
    private Long userId;
    private MembershipLevel previousLevel;
    private MembershipLevel newLevel;
    private String changeReason;
    private LocalDateTime changedAt;
} 