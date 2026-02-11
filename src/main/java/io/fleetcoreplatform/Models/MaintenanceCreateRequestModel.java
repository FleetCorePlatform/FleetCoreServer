package io.fleetcoreplatform.Models;

import java.util.UUID;

public record MaintenanceCreateRequestModel(UUID droneUuid, String type, String description) {}
