package io.fleetcoreplatform.Managers.Database.DbModels;

import io.fleetcoreplatform.Models.DroneHomePositionModel;
import java.sql.Timestamp;
import java.util.UUID;
import org.postgis.Point;

public class DbDrone {
    private UUID uuid;
    private String name;
    private UUID group_uuid;
    private String address;
    private String manager_version;
    private Timestamp first_discovered;
    private DroneHomePositionModel home_position;

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

    public UUID getGroup_uuid() {
        return group_uuid;
    }

    public void setGroup_uuid(UUID group_uuid) {
        this.group_uuid = group_uuid;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getManager_version() {
        return manager_version;
    }

    public void setManager_version(String manager_version) {
        this.manager_version = manager_version;
    }

    public Timestamp getFirst_discovered() {
        return first_discovered;
    }

    public void setFirst_discovered(Timestamp first_discovered) {
        this.first_discovered = first_discovered;
    }

    public DroneHomePositionModel getHome_position() {
        return home_position;
    }

    public void setHome_position(Point home_position) {
        this.home_position =
                new DroneHomePositionModel(home_position.x, home_position.y, home_position.z);
    }
}
