package io.fleetcoreplatform.Models;

import java.util.List;
import java.util.UUID;
import org.postgis.Point;

public record CreateMissionRequestModel(
        String jobName,
        UUID outpostUuid,
        UUID groupUuid,
        List<UUID> droneUuids,
        Point[] waypoints,
        Integer altitude,
        Integer speed
) {}