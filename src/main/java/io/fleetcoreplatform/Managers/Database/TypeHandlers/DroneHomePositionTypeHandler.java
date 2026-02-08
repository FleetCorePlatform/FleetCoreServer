package io.fleetcoreplatform.Managers.Database.TypeHandlers;

import io.fleetcoreplatform.Models.DroneHomePositionModel;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgis.PGgeometry;
import org.postgis.Point;

@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes({DroneHomePositionModel.class})
public class DroneHomePositionTypeHandler extends BaseTypeHandler<DroneHomePositionModel> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, DroneHomePositionModel parameter, JdbcType jdbcType) throws SQLException {
        Point point = new Point(parameter.x(), parameter.y(), parameter.z());
        point.setSrid(4326);
        ps.setObject(i, new PGgeometry(point));
    }

    @Override
    public DroneHomePositionModel getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return extractPosition(rs.getObject(columnName));
    }

    @Override
    public DroneHomePositionModel getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return extractPosition(rs.getObject(columnIndex));
    }

    @Override
    public DroneHomePositionModel getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return extractPosition(cs.getObject(columnIndex));
    }

    private DroneHomePositionModel extractPosition(Object obj) throws SQLException {
        if (obj == null) return null;

        if (obj instanceof PGgeometry) {
            Point point = (Point) ((PGgeometry) obj).getGeometry();
            return new DroneHomePositionModel(point.getX(), point.getY(), point.getZ());
        }

        if (obj instanceof org.postgresql.util.PGobject) {
            String value = ((org.postgresql.util.PGobject) obj).getValue();
            Point point = (Point) PGgeometry.geomFromString(value);
            return new DroneHomePositionModel(point.getX(), point.getY(), point.getZ());
        }

        throw new SQLException("Unexpected geometry type: " + obj.getClass().getName());
    }
}