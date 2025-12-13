package io.fleetcoreplatform.Models;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import software.amazon.awssdk.services.iot.model.JobExecutionStatus;

public record DroneExecutionStatusResponseModel(
        UUID droneUUID, JobExecutionStatus status, Timestamp startedAtt) {}
