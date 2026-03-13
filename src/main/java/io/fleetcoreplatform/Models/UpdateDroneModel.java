package io.fleetcoreplatform.Models;

public record UpdateDroneModel(
        String droneName,
        String address,
        String agentVersion,
        DroneHomePositionModel homePosition
) {}