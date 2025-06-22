package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;

import java.util.List;

@Getter
@Setter
public class UserStatusResponse {
    private Long userId;
    private MembershipLevel membershipLevel;
    private List<String> badges;
}
