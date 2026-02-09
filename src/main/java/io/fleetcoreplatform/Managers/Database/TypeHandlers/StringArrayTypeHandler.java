package io.fleetcoreplatform.Managers.Database.TypeHandlers;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.ARRAY);
        } else {
            String[] asArray = parameter.toArray(new String[0]);
            Array sqlArray = ps.getConnection().createArrayOf("varchar", asArray);
            ps.setArray(i, sqlArray);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseSqlArray(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseSqlArray(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseSqlArray(cs.getArray(columnIndex));
    }

    private List<String> parseSqlArray(Array sqlArray) throws SQLException {
        if (sqlArray == null) return null;
        String[] array = (String[]) sqlArray.getArray();
        return Arrays.asList(array);
    }
}