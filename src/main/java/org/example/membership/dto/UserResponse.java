package org.example.membership.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.membership.common.enums.MembershipLevel;
import org.example.membership.entity.User;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private MembershipLevel membershipLevel;
    private LocalDateTime lastMembershipChange;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getMembershipLevel(),
                user.getLastMembershipChange(),
                user.getCreatedAt()
        );
    }
}
