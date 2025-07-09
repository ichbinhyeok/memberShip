package org.example.membership.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.membership.common.enums.CouponAmount;

import java.math.BigDecimal;

@Converter(autoApply = true)
public class CouponAmountConverter implements AttributeConverter<CouponAmount, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(CouponAmount attribute) {
        return attribute != null ? attribute.getAmount() : null;
    }

    @Override
    public CouponAmount convertToEntityAttribute(BigDecimal dbData) {
        if (dbData == null) return null;
        return CouponAmount.fromAmount(dbData);
    }
}