package io.fleetcoreplatform.Managers.Database.DbModels;

import java.sql.Timestamp;
import java.util.UUID;
import org.postgis.Point;

public class DbDetections {
    private UUID uuid;
    private UUID mission_uuid;
    private UUID detected_by_drone_uuid;
    private Timestamp detected_at;
    private Point location;
    private String image_key;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getMission_uuid() {
        return mission_uuid;
    }

    public void setMission_uuid(UUID mission_uuid) {
        this.mission_uuid = mission_uuid;
    }

    public UUID getDetected_by_drone_uuid() {
        return detected_by_drone_uuid;
    }

    public void setDetected_by_drone_uuid(UUID detected_by_drone_uuid) {
        this.detected_by_drone_uuid = detected_by_drone_uuid;
    }

    public Timestamp getDetected_at() {
        return detected_at;
    }

    public void setDetected_at(Timestamp detected_at) {
        this.detected_at = detected_at;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public String getImage_key() {
        return image_key;
    }

    public void setImage_key(String image_key) {
        this.image_key = image_key;
    }
}
