package io.fleetcoreplatform.Models;

import java.sql.Timestamp;
import java.util.UUID;

public class DroneSummaryModel {
    private UUID uuid;
    private String name;
    private String group_name;
    private String address;
    private String manager_version;
    private Timestamp first_discovered;
    private DroneHomePositionModel home_position;
    private Boolean maintenance;
    private Double remaining_percent;
    private Boolean inFlight;

    public DroneSummaryModel() {}

    public DroneSummaryModel(UUID uuid, String name, String group_name, String address,
                            String manager_version, Timestamp first_discovered,
                            DroneHomePositionModel home_position, Boolean maintenance,
                            Double remaining_percent, Boolean inFlight) {
        this.uuid = uuid;
        this.name = name;
        this.group_name = group_name;
        this.address = address;
        this.manager_version = manager_version;
        this.first_discovered = first_discovered;
        this.home_position = home_position;
        this.maintenance = maintenance;
        this.remaining_percent = remaining_percent;
        this.inFlight = inFlight;
    }

    // Getters - JavaBeans style
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public String getGroup_name() { return group_name; }
    public String getAddress() { return address; }
    public String getManager_version() { return manager_version; }
    public Timestamp getFirst_discovered() { return first_discovered; }
    public DroneHomePositionModel getHome_position() { return home_position; }
    public Boolean getMaintenance() { return maintenance; }
    public Double getRemaining_percent() { return remaining_percent; }
    public Boolean getInFlight() { return inFlight; }

    // Setters
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public void setName(String name) { this.name = name; }
    public void setGroup_name(String group_name) { this.group_name = group_name; }
    public void setAddress(String address) { this.address = address; }
    public void setManager_version(String manager_version) { this.manager_version = manager_version; }
    public void setFirst_discovered(Timestamp first_discovered) { this.first_discovered = first_discovered; }
    public void setHome_position(DroneHomePositionModel home_position) { this.home_position = home_position; }
    public void setMaintenance(Boolean maintenance) { this.maintenance = maintenance; }
    public void setRemaining_percent(Double remaining_percent) { this.remaining_percent = remaining_percent; }
    public void setInFlight(Boolean inFlight) { this.inFlight = inFlight; }
}