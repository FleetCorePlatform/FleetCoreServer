package io.fleetcoreplatform.Models;

import java.util.UUID;

public record CreateMissionRequestModel(
        UUID outpostUuid, UUID groupUuid, int altitude, String jobName) {}
