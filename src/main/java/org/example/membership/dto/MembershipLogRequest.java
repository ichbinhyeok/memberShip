package org.example.membership.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MembershipLogRequest {
    private Long userId;
    private MembershipLevel previousLevel;
    private MembershipLevel newLevel;
    private String changeReason;
    private LocalDateTime changedAt;
}