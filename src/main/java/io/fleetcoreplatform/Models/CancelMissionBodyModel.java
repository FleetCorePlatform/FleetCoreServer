package io.fleetcoreplatform.Models;

public record CancelMissionBodyModel(MissionBodyEnum status) {
    public enum MissionBodyEnum {
        CANCELLED
    }
}
