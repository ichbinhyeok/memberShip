package org.example.membership.dto;

import java.time.LocalDateTime;

// UserDto.java
public class UserDto {
    public Long id;
    public String name;
    public String membershipLevel;
    public LocalDateTime lastMembershipChange;
    public LocalDateTime createdAt;
}
