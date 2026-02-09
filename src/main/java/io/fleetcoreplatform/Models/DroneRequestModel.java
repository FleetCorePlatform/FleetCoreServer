package io.fleetcoreplatform.Models;

import java.util.List;

public record DroneRequestModel(
        String groupName,
        String droneName,
        String address,
        String agentVersion,
        DroneHomePositionModel homePosition,
        String model,
        List<String> capabilities
) {}
