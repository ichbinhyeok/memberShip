package org.example.membership.dto;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.membership.common.enums.MembershipLevel;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipLevel membershipLevel = MembershipLevel.SILVER;

}
