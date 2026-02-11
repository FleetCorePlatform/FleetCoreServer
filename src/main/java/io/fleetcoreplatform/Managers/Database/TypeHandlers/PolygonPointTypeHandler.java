package io.fleetcoreplatform.Managers.Database.TypeHandlers;

import io.fleetcoreplatform.Models.PolygonPoint2DModel;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(PolygonPoint2DModel.class)
public class PolygonPointTypeHandler extends BaseTypeHandler<PolygonPoint2DModel> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PolygonPoint2DModel parameter, JdbcType jdbcType) throws SQLException {
        Point point = new Point(parameter.x(), parameter.y());
        point.setSrid(4326);
        ps.setObject(i, new PGgeometry(point));
    }

    @Override
    public PolygonPoint2DModel getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convertToModel(rs.getObject(columnName));
    }

    @Override
    public PolygonPoint2DModel getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convertToModel(rs.getObject(columnIndex));
    }

    @Override
    public PolygonPoint2DModel getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convertToModel(cs.getObject(columnIndex));
    }

    private PolygonPoint2DModel convertToModel(Object obj) throws SQLException {
        if (obj == null) return null;

        Geometry geometry = null;

        if (obj instanceof PGgeometry) {
            geometry = ((PGgeometry) obj).getGeometry();
        }
        else if (obj instanceof org.postgresql.util.PGobject) {
            String value = ((org.postgresql.util.PGobject) obj).getValue();
            if (value != null) {
                geometry = PGgeometry.geomFromString(value);
            }
        }

        if (geometry instanceof Point p) {
            return new PolygonPoint2DModel(p.getX(), p.getY());
        }

        return null;
    }
}