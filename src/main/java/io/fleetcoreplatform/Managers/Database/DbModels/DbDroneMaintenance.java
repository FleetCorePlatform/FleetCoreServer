package io.fleetcoreplatform.Managers.Database.DbModels;

import java.sql.Timestamp;
import java.util.UUID;

public class DbDroneMaintenance {
    private UUID uuid;
    private UUID drone_uuid;
    private UUID performed_by;
    private String maintenance_type;
    private String description;
    private Timestamp performed_at;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getDrone_uuid() {
        return drone_uuid;
    }

    public void setDrone_uuid(UUID drone_uuid) {
        this.drone_uuid = drone_uuid;
    }

    public UUID getPerformed_by() {
        return performed_by;
    }

    public void setPerformed_by(UUID performed_by) {
        this.performed_by = performed_by;
    }

    public String getMaintenance_type() {
        return maintenance_type;
    }

    public void setMaintenance_type(String maintenance_type) {
        this.maintenance_type = maintenance_type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getPerformed_at() {
        return performed_at;
    }

    public void setPerformed_at(Timestamp performed_at) {
        this.performed_at = performed_at;
    }
}
