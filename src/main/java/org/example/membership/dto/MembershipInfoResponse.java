package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;

import java.time.LocalDateTime;

@Getter
@Setter
public class MembershipInfoResponse {
    private Long userId;
    private String userName;
    private MembershipLevel currentLevel;
    private LocalDateTime lastMembershipChange;
} 