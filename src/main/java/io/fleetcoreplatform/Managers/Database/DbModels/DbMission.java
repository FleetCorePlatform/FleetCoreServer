package io.fleetcoreplatform.Managers.Database.DbModels;

import java.sql.Timestamp;
import java.util.UUID;

public class DbMission {
    private UUID uuid;
    private UUID group_uuid;
    private String name;
    private String bundle_url;
    private Timestamp start_time;
    private UUID created_by;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getGroup_uuid() {
        return group_uuid;
    }

    public void setGroup_uuid(UUID group_uuid) {
        this.group_uuid = group_uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBundle_url() {
        return bundle_url;
    }

    public void setBundle_url(String bundle_url) {
        this.bundle_url = bundle_url;
    }

    public Timestamp getStart_time() {
        return start_time;
    }

    public void setStart_time(Timestamp start_time) {
        this.start_time = start_time;
    }

    public UUID getCreated_by() {
        return created_by;
    }

    public void setCreated_by(UUID created_by) {
        this.created_by = created_by;
    }
}
