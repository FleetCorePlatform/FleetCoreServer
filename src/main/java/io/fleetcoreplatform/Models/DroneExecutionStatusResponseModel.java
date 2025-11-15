package io.fleetcoreplatform.Models;

import java.time.Instant;
import java.util.UUID;
import software.amazon.awssdk.services.iot.model.JobExecutionStatus;

public record DroneExecutionStatusResponseModel(
        UUID drone_uuid, JobExecutionStatus status, Instant started_at) {}
