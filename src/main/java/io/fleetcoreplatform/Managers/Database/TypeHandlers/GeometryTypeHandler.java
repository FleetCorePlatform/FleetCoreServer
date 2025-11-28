package io.fleetcoreplatform.Managers.Database.TypeHandlers;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgis.Geometry;
import org.postgis.PGgeometry;

@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes({Geometry.class})
public class GeometryTypeHandler extends BaseTypeHandler<Geometry> {
    @Override
    public void setNonNullParameter(
            PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, new PGgeometry(parameter));
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return extractGeometry(rs.getObject(columnName));
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return extractGeometry(rs.getObject(columnIndex));
    }

    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return extractGeometry(cs.getObject(columnIndex));
    }

    private Geometry extractGeometry(Object obj) throws SQLException {
        if (obj == null) return null;

        if (obj instanceof PGgeometry) {
            return ((PGgeometry) obj).getGeometry();
        }

        if (obj instanceof org.postgresql.util.PGobject) {
            String value = ((org.postgresql.util.PGobject) obj).getValue();
            assert value != null;
            return PGgeometry.geomFromString(value);
        }

        throw new SQLException("Unexpected geometry type: " + obj.getClass().getName());
    }
}
