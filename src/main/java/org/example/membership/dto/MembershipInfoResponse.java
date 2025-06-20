package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;

import java.time.LocalDateTime;

@Getter
@Setter
public class MembershipInfoResponse {
    private Long userId;
    private String userName;
    private MembershipLevel currentLevel;
    private LocalDateTime lastMembershipChange;

    public static MembershipInfoResponse from(User user) {
        MembershipInfoResponse dto = new MembershipInfoResponse();
        dto.setUserId(user.getId());
        dto.setUserName(user.getName());
        dto.setCurrentLevel(user.getMembershipLevel());
        dto.setLastMembershipChange(user.getLastMembershipChange());
        return dto;
    }
} 