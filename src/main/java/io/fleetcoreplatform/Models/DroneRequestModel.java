package io.fleetcoreplatform.Models;

import java.util.List;
import java.util.UUID;

public record DroneRequestModel(
        UUID groupName,
        String droneName,
        String address,
        String agentVersion,
        DroneHomePositionModel homePosition,
        String model,
        List<String> capabilities
) {}
