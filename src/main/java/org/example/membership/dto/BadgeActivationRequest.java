package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BadgeActivationRequest {
    private Long userId;
    private Long categoryId;
    private boolean active; // true = 활성화, false = 비활성화


}
