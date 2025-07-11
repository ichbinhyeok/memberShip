package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualBadgeActivationRequest {
    private Long userId;
    private Long categoryId;
}
