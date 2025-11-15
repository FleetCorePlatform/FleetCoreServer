package io.fleetcoreplatform.Managers.Database.DbModels;

import java.sql.Timestamp;
import java.util.UUID;

public class DbLogFile {
    private UUID uuid;
    private UUID drone_uuid;
    private Timestamp created_at;
    private Boolean archived;
    private Timestamp archived_date;

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

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Timestamp getArchived_date() {
        return archived_date;
    }

    public void setArchived_date(Timestamp archived_date) {
        this.archived_date = archived_date;
    }
}
