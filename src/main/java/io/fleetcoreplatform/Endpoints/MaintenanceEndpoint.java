package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDrone;
import io.fleetcoreplatform.Managers.Database.DbModels.DbDroneMaintenance;
import io.fleetcoreplatform.Managers.Database.Mappers.CoordinatorMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.DroneMaintenanceMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.DroneMapper;
import io.fleetcoreplatform.Models.MaintenanceCreateRequestModel;
import io.fleetcoreplatform.Models.MaintenanceSummary;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import java.sql.Timestamp;
import java.time.Instant;
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

@NoCache
@Path("/api/v1/maintenance")
@Tag(name = "Maintenance", description = "Operations related to drone maintenance")
public class MaintenanceEndpoint {
    @Inject SecurityIdentity identity;
    @Inject Logger logger;
    @Inject DroneMaintenanceMapper maintenanceMapper;
    @Inject DroneMapper droneMapper;
    @Inject CoordinatorMapper coordinatorMapper;

    @GET
    @Operation(summary = "Get maintenances", description = "List maintenance records for an outpost")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.ARRAY, implementation = MaintenanceSummary.class))),
        @APIResponse(responseCode = "400", description = "Bad request"),
        @APIResponse(responseCode = "404", description = "Not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getMaintenances(
            @Parameter(description = "UUID of the outpost", required = true)
            @QueryParam("outpost_uuid") UUID outpost_uuid) {
        if (outpost_uuid == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        try {
            List<MaintenanceSummary> maintenances = maintenanceMapper.listByOutpostAndCoordinator(outpost_uuid, cognitoSub);

            if (maintenances == null || maintenances.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(maintenances).build();

        } catch (Exception e) {
            logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Operation(summary = "Schedule maintenance", description = "Schedule a new maintenance for a drone")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Maintenance scheduled successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Drone not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response scheduleMaintenance(
            @RequestBody(description = "Maintenance schedule details", required = true)
            MaintenanceCreateRequestModel request) {
        if (request.droneUuid() == null || request.type() == null || request.description() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        DbDrone drone = droneMapper.findByUuidAndCoordinator(request.droneUuid(), cognitoSub);
        if (drone == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            maintenanceMapper.insert(UUID.randomUUID(), request.droneUuid(), null, request.type(), request.description(), new Timestamp(System.currentTimeMillis()), null);
            return Response.noContent().build();
        } catch (Exception e) {
            logger.error(e);
            return Response.serverError().build();
        }
    }

    @PATCH
    @Path("/{maintenance_uuid}")
    @Operation(summary = "Complete maintenance", description = "Mark a maintenance record as complete")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Maintenance completed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DbDroneMaintenance.class))),
        @APIResponse(responseCode = "404", description = "Maintenance not found"),
        @APIResponse(responseCode = "409", description = "Maintenance already completed"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response completeMaintenance(
            @Parameter(description = "UUID of the maintenance record", required = true)
            @PathParam("maintenance_uuid") UUID maintenanceUuid) {
        String cognitoSub = identity.getPrincipal().getName();

        DbDroneMaintenance maintenance = maintenanceMapper.findByUuidAndCoordinator(maintenanceUuid, cognitoSub);

        if (maintenance == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (maintenance.getPerformed_at() != null) {
            return Response.status(Response.Status.CONFLICT).entity("Already completed").build();
        }

        UUID coordinatorUuid = coordinatorMapper.findByCognitoSub(cognitoSub).getUuid();

        maintenance.setPerformed_by(coordinatorUuid);
        maintenance.setPerformed_at(new Timestamp(System.currentTimeMillis()));

        try {
            maintenanceMapper.markAsComplete(maintenance);
            return Response.ok(maintenance).build();
        } catch (Exception e) {
            logger.error(e);
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("/{maintenance_uuid}")
    @Operation(summary = "Delete maintenance", description = "Delete a maintenance record")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Maintenance deleted successfully"),
        @APIResponse(responseCode = "404", description = "Maintenance not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response deleteMaintenance(
            @Parameter(description = "UUID of the maintenance record", required = true)
            @PathParam("maintenance_uuid") UUID maintenance_uuid) {
        String cognitoSub = identity.getPrincipal().getName();

        DbDroneMaintenance record = maintenanceMapper.findByUuidAndCoordinator(maintenance_uuid, cognitoSub);
        if (record == null) return Response.status(Response.Status.NOT_FOUND).build();

        maintenanceMapper.delete(maintenance_uuid);

        return Response.noContent().build();
    }
}
