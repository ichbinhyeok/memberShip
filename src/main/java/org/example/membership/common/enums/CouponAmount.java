package org.example.membership.common.enums;

import java.math.BigDecimal;

public enum CouponAmount {
    W0(new BigDecimal("0")),
    W1000(new BigDecimal("1000")),
    W5000(new BigDecimal("5000")),
    W10000(new BigDecimal("10000")),
    W15000(new BigDecimal("15000")),
    W20000(new BigDecimal("20000"));

    private final BigDecimal amount;

    CouponAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public static CouponAmount fromAmount(BigDecimal value) {
        for (CouponAmount ca : values()) {
            if (ca.amount.compareTo(value) == 0) {
                return ca;
            }
        }
        throw new IllegalArgumentException("Unknown coupon amount: " + value);
    }
}