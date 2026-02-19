package io.fleetcoreplatform.Endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fleetcoreplatform.Exceptions.GroupHasNoOutpostException;
import io.fleetcoreplatform.Exceptions.KinesisCannotCreateChannelException;
import io.fleetcoreplatform.Managers.Database.DbModels.DbDrone;
import io.fleetcoreplatform.Managers.Database.Mappers.DroneMapper;
import io.fleetcoreplatform.Managers.IoTCore.IotDataPlaneManager;
import io.fleetcoreplatform.Managers.IoTCore.IotManager;
import io.fleetcoreplatform.Models.*;
import io.fleetcoreplatform.Services.CoreService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.camel.Body;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@NoCache
@Path("/api/v1/drones")
@RolesAllowed("${allowed.role-name}")
@Tag(name = "Drones", description = "Operations related to drone management")
public class DronesEndpoint {
    @Inject CoreService coreService;
    @Inject DroneMapper droneMapper;
    @Inject IotManager iotManager;
    @Inject IotDataPlaneManager iotPublisher;
    @Inject SecurityIdentity identity;
    Logger logger = Logger.getLogger(DronesEndpoint.class.getName());

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    @Operation(summary = "List drones", description = "List drones for a specific group")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Success",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        type = SchemaType.ARRAY,
                                                        implementation = DbDrone.class))),
                @APIResponse(responseCode = "400", description = "Bad request"),
                @APIResponse(responseCode = "500", description = "Internal server error")
            })
    public Response listGroupDrones(
            @Parameter(description = "Limit the number of results", required = false)
            @DefaultValue("10") @QueryParam("limit") int limit,
            @Parameter(description = "UUID of the group", required = true)
            @Nullable @QueryParam("group_id") UUID group_uuid) {

        if (group_uuid == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        if (limit <= 0 || limit > 1000) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            List<DbDrone> drones = droneMapper.listDronesByGroupAndCoordinator(group_uuid, cognitoSub, limit);
            if (drones == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(drones).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{drone_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 25, window = 1, windowUnit = ChronoUnit.SECONDS)
    @Operation(summary = "Get drone details", description = "Get detailed information about a specific drone")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DroneFullModel.class))),
        @APIResponse(responseCode = "404", description = "Drone not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getDrone(
            @Parameter(description = "UUID of the drone", required = true)
            @PathParam("drone_uuid") UUID drone_uuid) {
        String cognitoSub = identity.getPrincipal().getName();

        try {
            DbDrone drone = droneMapper.findByUuidAndCoordinator(drone_uuid, cognitoSub);
            if (drone == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            var uptime = iotManager.getDroneStatus(drone.getName());

            var response = new DroneFullModel(drone.getUuid(), drone.getName(), drone.getGroup_uuid(), drone.getAddress(), drone.getManager_version(), drone.getFirst_discovered(), drone.getHome_position(), drone.getModel(), drone.getCapabilities(), uptime);
            return Response.ok(response).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    @Operation(summary = "Register a new drone", description = "Register a new drone to the platform")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Drone registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisteredDroneResponse.class))),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Related resource not found"),
        @APIResponse(responseCode = "422", description = "Group has no outpost"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response registerDrone(
            @RequestBody(description = "Drone registration details", required = true)
            DroneRequestModel body) {
        if (body == null
                || body.groupName() == null
                || body.droneName() == null
                || body.address() == null
                || body.agentVersion() == null
                || body.homePosition() == null
                || body.homePosition().x() == null
                || body.homePosition().y() == null
                || body.homePosition().z() == null
                || body.model() == null
                || body.capabilities() == null
                || body.capabilities().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            RegisteredDroneResponse certs =
                    coreService.registerNewDrone(
                            body.groupName(),
                            body.droneName(),
                            body.address(),
                            body.agentVersion(),
                            body.homePosition(),
                            body.model(),
                            body.capabilities());

            return Response.ok(certs).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (GroupHasNoOutpostException noe) {
            logger.severe(noe.getMessage());
            return Response.status(422).entity(noe).build();
        } catch (KinesisCannotCreateChannelException kce) {
            logger.severe(kce.getMessage());
            return Response.notModified().build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    @Path("/{drone_uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    @Operation(summary = "Update drone", description = "Update details of an existing drone")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Drone updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Drone not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response updateDrone(
            @Parameter(description = "UUID of the drone", required = true)
            @PathParam("drone_uuid") UUID droneUuid,
            @RequestBody(description = "Drone update details", required = true)
            DroneRequestModel body) {
        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        DbDrone droneCheck = droneMapper.findByUuidAndCoordinator(droneUuid, cognitoSub);
        if (droneCheck == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            coreService.updateDrone(droneUuid, body);

            return Response.noContent().build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{drone_uuid}/group")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    @Operation(summary = "Remove drone from group", description = "Remove a drone from its assigned group")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Drone removed from group successfully"),
        @APIResponse(responseCode = "404", description = "Drone not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response ungroupDrone(
            @Parameter(description = "UUID of the drone", required = true)
            @PathParam("drone_uuid") UUID drone_uuid) {
        try {
            coreService.removeDroneFromGroup(drone_uuid);

            return Response.noContent().build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/{drone_uuid}/group")
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Assign drone to group", description = "Assign a drone to a specific group")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Drone assigned to group successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Drone or group not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response ungroupDrone(
            @Parameter(description = "UUID of the drone", required = true)
            @PathParam("drone_uuid") UUID drone_uuid,
            @RequestBody(description = "Group assignment details", required = true)
            SetDroneGroupRequestModel body) {
        if (body == null || body.group_uuid() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            coreService.addDroneToGroup(drone_uuid, body.group_uuid());

            return Response.noContent().build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{drone_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    @Operation(summary = "Delete drone", description = "Permanently delete a drone")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Drone deleted successfully"),
        @APIResponse(responseCode = "404", description = "Drone not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response deleteDrone(
            @Parameter(description = "UUID of the drone", required = true)
            @PathParam("drone_uuid") UUID droneUuid) {
        String cognitoSub = identity.getPrincipal().getName();

        DbDrone droneCheck = droneMapper.findByUuidAndCoordinator(droneUuid, cognitoSub);
        if (droneCheck == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            coreService.removeDrone(droneUuid);

            return Response.noContent().build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/{drone_uuid}/stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Set drone streaming", description = "Enable or disable drone video streaming")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Streaming state updated successfully"),
        @APIResponse(responseCode = "304", description = "Not modified (Json error)"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Drone not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response setDroneStreaming(
            @Parameter(description = "UUID of the drone", required = true)
            @PathParam("drone_uuid") UUID droneUuid,
            @RequestBody(description = "Streaming control details", required = true)
            @Body DroneStreamingRequestModel body) {
        if (body == null || body.enabled() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();
        DbDrone droneCheck = droneMapper.findByUuidAndCoordinator(droneUuid, cognitoSub);

        if (droneCheck == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            String jsonString = mapper.writeValueAsString(body);
            iotPublisher.publish("devices/" + droneUuid.toString() + "/stream", jsonString, 1);

            return Response.noContent().build();
        } catch (JsonProcessingException e) {
            logger.severe(e.getMessage());
            return Response.notModified().build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.serverError().build();
        }
    }
}
