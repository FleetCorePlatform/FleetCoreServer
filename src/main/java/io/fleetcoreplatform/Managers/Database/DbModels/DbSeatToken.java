package io.fleetcoreplatform.Managers.Database.DbModels;

import java.sql.Timestamp;
import java.util.UUID;

public class DbSeatToken {
    private UUID uuid;
    private UUID created_by;
    private UUID group;
    private Timestamp created_at;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getCreated_by() {
        return created_by;
    }

    public void setCreated_by(UUID created_by) {
        this.created_by = created_by;
    }

    public UUID getGroup() {
        return group;
    }

    public void setGroup(UUID group) {
        this.group = group;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }
}
