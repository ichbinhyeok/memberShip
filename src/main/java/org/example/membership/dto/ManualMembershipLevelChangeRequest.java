package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;

@Getter
@Setter
public class ManualMembershipLevelChangeRequest {
    private Long userId;
    private MembershipLevel newLevel;
}
