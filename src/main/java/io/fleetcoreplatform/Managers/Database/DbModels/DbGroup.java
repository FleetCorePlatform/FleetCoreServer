package io.fleetcoreplatform.Managers.Database.DbModels;

import java.sql.Timestamp;
import java.util.UUID;

public class DbGroup {
    private UUID uuid;
    private UUID outpost_uuid;
    private String name;
    private Timestamp created_at;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getOutpost_uuid() {
        return outpost_uuid;
    }

    public void setOutpost_uuid(UUID outpost_uuid) {
        this.outpost_uuid = outpost_uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }
}
