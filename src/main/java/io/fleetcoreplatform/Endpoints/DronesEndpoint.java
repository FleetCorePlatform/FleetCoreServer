package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDrone;
import io.fleetcoreplatform.Managers.Database.Mappers.DroneMapper;
import io.fleetcoreplatform.Models.DroneRequestModel;
import io.fleetcoreplatform.Models.IoTCertContainer;
import io.fleetcoreplatform.Models.SetDroneGroupRequestModel;
import io.fleetcoreplatform.Services.CoreService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.NoCache;

// TODO: Implement row-level authentication on drones endpoint (#29)

@NoCache
@Path("/api/v1/drones")
@RolesAllowed("${allowed.role-name}")
public class DronesEndpoint {
    @Inject CoreService coreService;
    @Inject DroneMapper droneMapper;
    @Inject SecurityIdentity identity;
    Logger logger = Logger.getLogger(DronesEndpoint.class.getName());

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
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
            @DefaultValue("10") @QueryParam("limit") int limit,
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
    public Response getDrone(@PathParam("drone_uuid") UUID drone_uuid) {
        String cognitoSub = identity.getPrincipal().getName();

        try {
            DbDrone drone = droneMapper.findByUuidAndCoordinator(drone_uuid, cognitoSub);
            if (drone == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(drone).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    public Response registerDrone(DroneRequestModel body) {
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
            IoTCertContainer certs =
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
    public Response updateDrone(@PathParam("drone_uuid") UUID droneUuid, DroneRequestModel body) {
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
    public Response ungroupDrone(@PathParam("drone_uuid") UUID drone_uuid) {
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
    public Response ungroupDrone(
            @PathParam("drone_uuid") UUID drone_uuid, SetDroneGroupRequestModel body) {
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
    public Response deleteDrone(@PathParam("drone_uuid") UUID droneUuid) {
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
}
