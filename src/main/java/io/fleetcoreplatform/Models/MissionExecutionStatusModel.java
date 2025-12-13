package io.fleetcoreplatform.Models;

import software.amazon.awssdk.services.iot.model.JobStatus;

import java.sql.Timestamp;

public record MissionExecutionStatusModel(JobStatus jobStatus, Timestamp startedAt, Timestamp finishedAt) {}
