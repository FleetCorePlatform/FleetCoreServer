package io.fleetcoreplatform.Models;

import java.util.UUID;

public record OutpostGroupSummary(UUID groupUUID, String groupName, int groupDroneCount) {}
