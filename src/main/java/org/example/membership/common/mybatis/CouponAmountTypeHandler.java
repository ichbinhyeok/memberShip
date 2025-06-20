package org.example.membership.common.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.example.membership.common.enums.CouponAmount;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CouponAmountTypeHandler extends BaseTypeHandler<CouponAmount> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, CouponAmount parameter, JdbcType jdbcType) throws SQLException {
        ps.setBigDecimal(i, parameter.getAmount());
    }

    @Override
    public CouponAmount getNullableResult(ResultSet rs, String columnName) throws SQLException {
        BigDecimal val = rs.getBigDecimal(columnName);
        return val == null ? null : CouponAmount.fromAmount(val);
    }

    @Override
    public CouponAmount getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        BigDecimal val = rs.getBigDecimal(columnIndex);
        return val == null ? null : CouponAmount.fromAmount(val);
    }

    @Override
    public CouponAmount getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        BigDecimal val = cs.getBigDecimal(columnIndex);
        return val == null ? null : CouponAmount.fromAmount(val);
    }
}