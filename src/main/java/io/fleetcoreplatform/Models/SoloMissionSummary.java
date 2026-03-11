package io.fleetcoreplatform.Models;

import java.sql.Timestamp;
import java.util.UUID;

public record SoloMissionSummary(
    String name,
    UUID missionUuid,
    Timestamp startTime,
    int detectionCount,
    UUID droneUuid,
    String droneName
) {}