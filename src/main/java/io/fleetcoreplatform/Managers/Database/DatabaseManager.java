package io.fleetcoreplatform.Managers.Database;

import io.fleetcoreplatform.Managers.Database.DbModels.DbOutpost;
import io.fleetcoreplatform.Managers.Database.Mappers.OutpostMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.*;
import org.postgis.Geometry;
import org.postgis.PGgeometry;

@ApplicationScoped
public class DatabaseManager {
    @Inject OutpostMapper outpostMapper;

    public Geometry getOutpostGeometry(String name) {
        DbOutpost row = outpostMapper.findByName(name);
        if (row == null) return null;

        String wkt = row.getArea();

        try {
            return PGgeometry.geomFromString(wkt);
        } catch (SQLException e) {
            System.err.println("Error parsing geometry: " + wkt);
        }

        return null;
    }
}
