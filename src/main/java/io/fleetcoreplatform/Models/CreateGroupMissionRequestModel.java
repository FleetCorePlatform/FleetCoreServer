package io.fleetcoreplatform.Models;

import java.util.List;
import java.util.UUID;

public record CreateGroupMissionRequestModel(
        String jobName,
        UUID outpostUuid,
        UUID groupUuid,
        List<UUID> droneUuids,
        Integer altitude,
        String scheduled
) {}