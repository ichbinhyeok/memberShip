package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.entity.User;

import java.util.Map;

@Getter
@Setter
public class BadgeUpdateRequest {
    private User user;
    private Map<Long, OrderCountAndAmount> statMap;

}