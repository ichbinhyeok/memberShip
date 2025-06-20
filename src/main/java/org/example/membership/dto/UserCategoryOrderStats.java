package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UserCategoryOrderStats {
    private Long userId;
    private Long categoryId;
    private Long orderCount;
    private BigDecimal totalAmount;
}