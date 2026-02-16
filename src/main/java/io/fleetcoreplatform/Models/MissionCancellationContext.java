package io.fleetcoreplatform.Models;

import java.util.UUID;

public record MissionCancellationContext(
    UUID missionUuid,
    UUID groupUuid,
    UUID outpostUuid
) {}