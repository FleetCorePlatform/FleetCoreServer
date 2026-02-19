package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.Database.DbModels.DbCoordinator;
import io.fleetcoreplatform.Managers.Database.Mappers.CoordinatorMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.MissionMapper;
import io.fleetcoreplatform.Models.*;
import io.fleetcoreplatform.Services.CoreService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.iot.model.IotException;

@Path("/api/v1/missions")
@RolesAllowed("${allowed.role-name}")
@Tag(name = "Missions", description = "Operations related to mission management")
public class MissionsEndpoint {
    @Inject CoreService coreService;
    @Inject MissionMapper missionMapper;
    @Inject SecurityIdentity identity;

    @Inject CoordinatorMapper coordinatorMapper;
    @Inject Logger logger;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 2, window = 10, windowUnit = ChronoUnit.MINUTES)
    @Operation(summary = "Start outpost survey", description = "Create and start a new mission to survey an outpost")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Mission created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MissionCreatedResponseModel.class))),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "401", description = "Unauthorized"),
        @APIResponse(responseCode = "404", description = "Resource not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response startOutpostSurvey(
            @RequestBody(description = "Mission creation details", required = true)
            CreateMissionRequestModel body) {
        if (body == null
                || body.outpostUuid() == null
                || body.groupUuid() == null
                || body.jobName() == null
                || body.jobName().length() > 64) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        DbCoordinator coordinator = coordinatorMapper.findByCognitoSub(cognitoSub);
        if (coordinator == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            UUID missionUUID =
                    coreService.createNewMission(
                            body.outpostUuid(),
                            body.groupUuid(),
                            coordinator.getUuid(),
                            body.altitude(),
                            body.jobName());

            return Response.ok(new MissionCreatedResponseModel(missionUUID)).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (IotException ioe) {
            return Response.status(500, "Server internal error while bundling mission instructions")
                    .build();
        } catch (Exception e) {
            logger.errorf("Unexpected error while starting outpost survey %s", e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    @Path("/{mission_uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cancel mission", description = "Cancel an ongoing mission")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Mission cancelled successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Mission not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response cancelMission(
            @Parameter(description = "UUID of the mission", required = true)
            @PathParam("mission_uuid") UUID missionUUID,
            @RequestBody(description = "Cancellation details", required = true)
            CancelMissionBodyModel body) {

        if (body == null || body.status() != CancelMissionBodyModel.MissionBodyEnum.CANCELLED) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Body must contain status: 'CANCELLED'")
                    .build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        try {
            MissionCancellationContext context = missionMapper.findCancellationContext(missionUUID, cognitoSub);
            if (context == null) {
                throw new NotFoundException("Mission not found or access denied");
            }

            coreService.cancelJob(
                context.outpostUuid(),
                context.groupUuid(),
                context.missionUuid()
            );

            return Response.noContent().build();

        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.errorf("Error cancelling mission %s: %s", missionUUID, e.getMessage());
            return Response.serverError().entity("Internal Server Error").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get mission summaries", description = "Get summaries of missions for a group or latest missions for coordinator")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.ARRAY, implementation = MissionSummary.class))),
        @APIResponse(responseCode = "204", description = "No content"),
        @APIResponse(responseCode = "400", description = "Bad request"),
        @APIResponse(responseCode = "404", description = "Not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getAllMissionSummariesForGroup(
            @Parameter(description = "UUID of the group", required = false)
            @QueryParam("group_uuid") UUID groupUuid,
            @Parameter(description = "Number of missions to retrieve", required = false)
            @QueryParam("count") Integer count) {
        if (groupUuid == null && count == null || groupUuid != null && count != null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        try {

            List<MissionSummary> missions;
            if (groupUuid != null) {
                missions = missionMapper.selectMissionSummariesByGroupAndCoordinator(groupUuid, cognitoSub);

            } else {
                missions = missionMapper.selectLatestMissionSummariesByCoordinator(cognitoSub, count);

            }
            if (missions == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            return Response.ok(missions).build();

        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get mission count", description = "Get total count of missions for the coordinator")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT, additionalProperties = Integer.class))),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getMissionCount() {
        String cognitoSub = identity.getPrincipal().getName();

        try {
            int totalMissions = missionMapper.countMissionsByCoordinator(cognitoSub);

            return Response.ok(Collections.singletonMap("count", totalMissions)).build();

        } catch (Exception e) {
            logger.error("Failed to count missions", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{mission_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get mission status", description = "Get detailed status of a specific mission")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MissionExecutionStatusModel.class))),
        @APIResponse(responseCode = "204", description = "No content"),
        @APIResponse(responseCode = "404", description = "Mission not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getMissionStatus(
            @Parameter(description = "UUID of the mission", required = true)
            @PathParam("mission_uuid") UUID missionUUID)
            throws NotFoundException {
        String cognitoSub = identity.getPrincipal().getName();

        try {
            MissionExecutionStatusModel status = coreService.getMissionStatus(missionUUID, cognitoSub);

            if (status == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }

            return Response.ok(status).build();

        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{mission_uuid}/{drone_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get drone mission status", description = "Get status of a specific drone in a mission")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DroneExecutionStatusResponseModel.class))),
        @APIResponse(responseCode = "204", description = "No content"),
        @APIResponse(responseCode = "404", description = "Mission or drone not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getThingMissionStatus(
            @Parameter(description = "UUID of the mission", required = true)
            @PathParam("mission_uuid") UUID missionUUID,
            @Parameter(description = "UUID of the drone", required = true)
            @PathParam("drone_uuid") UUID droneUUID)
            throws NotFoundException {
        String cognitoSub = identity.getPrincipal().getName();

        try {
            DroneExecutionStatusResponseModel status = coreService.getThingMissionStatus(droneUUID, missionUUID, cognitoSub);

            if (status == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }

            return Response.ok(status).build();

        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
