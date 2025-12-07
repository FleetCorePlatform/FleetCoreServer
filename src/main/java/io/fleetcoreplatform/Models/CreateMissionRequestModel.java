package io.fleetcoreplatform.Models;

import java.util.UUID;

public record CreateMissionRequestModel(
        String outpost, UUID groupUUID, UUID coordinatorUUID, int altitude) {}
