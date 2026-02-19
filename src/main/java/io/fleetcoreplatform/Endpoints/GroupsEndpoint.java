package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import io.fleetcoreplatform.Exceptions.GroupNotEmptyException;
import io.fleetcoreplatform.Managers.Database.DbModels.DbGroup;
import io.fleetcoreplatform.Managers.Database.DbModels.DbOutpost;
import io.fleetcoreplatform.Managers.Database.Mappers.GroupMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.OutpostMapper;
import io.fleetcoreplatform.Managers.SQS.SqsManager;
import io.fleetcoreplatform.Models.DroneSummaryModel;
import io.fleetcoreplatform.Models.DroneTelemetryModel;
import io.fleetcoreplatform.Models.GroupRequestModel;
import io.fleetcoreplatform.Models.UpdateGroupOutpostModel;
import io.fleetcoreplatform.Services.CoreService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.jboss.resteasy.reactive.NoCache;

@NoCache
@Path("/api/v1/groups")
@Tag(name = "Groups", description = "Operations related to drone groups")
public class GroupsEndpoint {
    @Inject GroupMapper groupMapper;
    @Inject OutpostMapper outpostMapper;
    @Inject CoreService coreService;
    @Inject SecurityIdentity identity;
    @Inject
    SqsManager sqsManager;

    @Inject Logger logger;

    @GET
    @Path("/{group_uuid}/drones")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    @Operation(summary = "List drones in a group", description = "Retrieves drones for a specific group with current telemetry data")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DroneSummaryModel.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "404", description = "Not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getDronesByGroup(
        @Parameter(description = "UUID of the group")
        @PathParam("group_uuid") UUID groupUuid,
        @Parameter(description = "Limit the number of results")
        @DefaultValue("10") @QueryParam("limit") Integer limit,
        @Context ApplicationConfig config
    ) {
        try {
            String cognitoSub = identity.getPrincipal().getName();
            List<DroneSummaryModel> drones = groupMapper.listDronesByGroupAndCoordinator(groupUuid, cognitoSub, limit);

            if (drones == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            List<DroneTelemetryModel> telemetryModels = sqsManager.ingestQueue(config.sqs().queueName());

            Map<String, DroneTelemetryModel> telemetryByDevice = telemetryModels.stream()
                .collect(Collectors.toMap(
                    DroneTelemetryModel::device_name,
                    Function.identity(),
                    (existing, replacement) -> replacement
                ));

            List<DroneSummaryModel> dronesWithTelemetry = drones.stream()
                .map(drone -> {
                    DroneTelemetryModel telem = telemetryByDevice.get(drone.getName());
                    if (telem != null) {
                        return new DroneSummaryModel(
                            drone.getUuid(),
                            drone.getName(),
                            drone.getGroup_name(),
                            drone.getAddress(),
                            drone.getManager_version(),
                            drone.getFirst_discovered(),
                            drone.getHome_position(),
                            drone.getMaintenance(),
                            telem.battery().remaining_percent(),
                            true,
                            drone.getSignaling_channel_name()
                        );
                    }
                    return new DroneSummaryModel(
                        drone.getUuid(),
                        drone.getName(),
                        drone.getGroup_name(),
                        drone.getAddress(),
                        drone.getManager_version(),
                        drone.getFirst_discovered(),
                        drone.getHome_position(),
                        drone.getMaintenance(),
                        null,
                        false,
                        drone.getSignaling_channel_name()
                    );
                })
                .toList();

            return Response.ok(dronesWithTelemetry).build();

        } catch (Exception e) {
            logger.error("Error while listing drones for group", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{group_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    @Operation(summary = "Get group details", description = "Get detailed information about a specific group")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DbGroup.class))),
        @APIResponse(responseCode = "404", description = "Group not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getGroup(
            @Parameter(description = "UUID of the group", required = true)
            @PathParam("group_uuid") UUID group_uuid) {
        try {
            DbGroup group = groupMapper.findByUuid(group_uuid);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(group).build();
        } catch (Exception e) {
            logger.error("Error fetching group details", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    @Operation(summary = "Create group", description = "Create a new drone group")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Group created successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Outpost not found"),
        @APIResponse(responseCode = "409", description = "Group name already exists"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response createGroup(
            @RequestBody(description = "Group creation details", required = true)
            GroupRequestModel group) {
        if (group == null || group.outpost_uuid() == null || group.group_name() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        DbGroup checkExists = groupMapper.findByName(group.group_name());
        if (checkExists != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        try {
            DbOutpost outpost = outpostMapper.findByUuid(group.outpost_uuid());

            if (outpost == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            coreService.createNewGroup(group.group_name(), group.outpost_uuid());

            return Response.status(Response.Status.CREATED).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{group_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    @Operation(summary = "Delete group", description = "Permanently delete a group")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Group deleted successfully"),
        @APIResponse(responseCode = "304", description = "Not modified (Group not empty)"),
        @APIResponse(responseCode = "404", description = "Group not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response deleteGroup(
            @Parameter(description = "UUID of the group", required = true)
            @PathParam("group_uuid") UUID group_uuid) {
        try {
            DbGroup group = groupMapper.findByUuid(group_uuid);

            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            coreService.tryDeleteGroup(group.getName());

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (GroupNotEmptyException gne) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    @Path("/{group_uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    @Operation(summary = "Update group", description = "Update group details")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Group updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Group not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response updateGroup(
            @Parameter(description = "UUID of the group", required = true)
            @PathParam("group_uuid") UUID group_uuid,
            @RequestBody(description = "Update details", required = true)
            UpdateGroupOutpostModel body) {
        if (body == null || body.outpost_uuid() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            coreService.updateGroup(group_uuid, body);

            return Response.noContent().build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
