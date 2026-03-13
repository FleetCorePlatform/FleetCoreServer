package io.fleetcoreplatform.Models;

import java.util.UUID;

public record CreateSoloMissionRequestModel(
        String jobName,
        UUID droneUuid,
        PolygonPoint2D[] waypoints,
        Integer altitude,
        Integer speed,
        Boolean returnToLaunch,
        String scheduled
) {}