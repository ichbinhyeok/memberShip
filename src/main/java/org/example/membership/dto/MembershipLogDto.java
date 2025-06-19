package org.example.membership.dto;

import java.time.LocalDateTime;

// MembershipLogDto.java
public class MembershipLogDto {
    public Long userId;
    public String previousLevel;
    public String newLevel;
    public String changeReason;
    public LocalDateTime changedAt;
}
