package com.xiangshang.consistent.extension.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.xiangshang.consistent.domain.model.ApiInvocation;
import com.xiangshang.consistent.extension.serializer.KryoSerializer;

/**
 * 序列化类型处理。{@link ApiInvocation}
 *
 * @author chenrg
 * @date 2019年1月28日
 */
public class SerializeTypeHandler extends BaseTypeHandler<ApiInvocation> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ApiInvocation parameter, JdbcType jdbcType)
            throws SQLException {
        if (Objects.isNull(parameter)) {
            return;
        }
        ps.setObject(i, KryoSerializer.serialize(parameter));
    }

    @Override
    public ApiInvocation getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        return convertBytesResult(bytes);
    }

    @Override
    public ApiInvocation getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] bytes = rs.getBytes(columnIndex);
        return convertBytesResult(bytes);
    }

    @Override
    public ApiInvocation getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] bytes = cs.getBytes(columnIndex);
        return convertBytesResult(bytes);
    }

    private ApiInvocation convertBytesResult(byte[] bytes) {
        if (ArrayUtils.isEmpty(bytes)) {
            return null;
        }
        return KryoSerializer.deSerialize(bytes, ApiInvocation.class);
    }

}
