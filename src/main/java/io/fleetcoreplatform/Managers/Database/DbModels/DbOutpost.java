package io.fleetcoreplatform.Managers.Database.DbModels;

import io.fleetcoreplatform.Models.OutpostAreaModel;
import io.fleetcoreplatform.Models.PolygonPoint2DModel;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.postgis.Geometry;
import org.postgis.Point;

public class DbOutpost {
    private UUID uuid;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private OutpostAreaModel area;
    private UUID created_by;
    private Timestamp created_at;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public OutpostAreaModel getArea() {
        return area;
    }

    public void setArea(Geometry area) {
        List<PolygonPoint2DModel> points = new ArrayList<>();

        for (int i = 0; i < area.numPoints(); i++) {
            Point point = area.getPoint(i);
            points.add(new PolygonPoint2DModel(point.x, point.y));
        }

        this.area = new OutpostAreaModel(points);
    }

    public UUID getCreated_by() {
        return created_by;
    }

    public void setCreated_by(UUID created_by) {
        this.created_by = created_by;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }
}
