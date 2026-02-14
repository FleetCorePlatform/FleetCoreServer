package io.fleetcoreplatform.Models;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public record DroneFullModel(
        UUID uuid,
        String name,
        UUID group_uuid,
        String address,
        String manager_version,
        Timestamp first_discovered,
        DroneHomePositionModel home_position,
        String model,
        List<String> capabilities,
        DroneStatusModel status
) {}