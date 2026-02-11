package io.fleetcoreplatform.Models;

import java.sql.Timestamp;
import java.util.UUID;

public class MissionSummary {
    private String name;
    private UUID missionUuid;
    private Timestamp startTime;
    private long detectionCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getMissionUuid() {
        return missionUuid;
    }

    public void setMissionUuid(UUID missionUuid) {
        this.missionUuid = missionUuid;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public long getDetectionCount() {
        return detectionCount;
    }

    public void setDetectionCount(long detectionCount) {
        this.detectionCount = detectionCount;
    }
}