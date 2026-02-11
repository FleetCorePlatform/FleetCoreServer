package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDrone;
import io.fleetcoreplatform.Managers.Database.DbModels.DbDroneMaintenance;
import io.fleetcoreplatform.Managers.Database.Mappers.CoordinatorMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.DroneMaintenanceMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.DroneMapper;
import io.fleetcoreplatform.Models.MaintenanceCreateRequestModel;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@NoCache
@Path("/api/v1/maintenance")
public class MaintenanceEndpoint {
    @Inject SecurityIdentity identity;
    @Inject Logger logger;
    @Inject DroneMaintenanceMapper maintenanceMapper;
    @Inject DroneMapper droneMapper;
    @Inject CoordinatorMapper coordinatorMapper;

    @GET
    public Response getMaintenances(@QueryParam("outpost_uuid") UUID outpost_uuid) {
        if (outpost_uuid == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        try {
            List<DbDroneMaintenance> maintenances = maintenanceMapper.listByOutpostAndCoordinator(outpost_uuid, cognitoSub);

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
    public Response scheduleMaintenance(MaintenanceCreateRequestModel request) {
        if (request.droneUuid() == null || request.type() == null || request.description() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        DbDrone drone = droneMapper.findByUuidAndCoordinator(request.droneUuid(), cognitoSub);
        if (drone == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            maintenanceMapper.insert(UUID.randomUUID(), request.droneUuid(), null, request.type(), request.description(), null);
            return Response.noContent().build();
        } catch (Exception e) {
            logger.error(e);
            return Response.serverError().build();
        }
    }

    @PATCH
    @Path("/{maintenance_uuid}")
    public Response completeMaintenance(@PathParam("maintenance_uuid") UUID maintenanceUuid) {
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
    public Response deleteMaintenance(@PathParam("maintenance_uuid") UUID maintenance_uuid) {
        String cognitoSub = identity.getPrincipal().getName();

        DbDroneMaintenance record = maintenanceMapper.findByUuidAndCoordinator(maintenance_uuid, cognitoSub);
        if (record == null) return Response.status(Response.Status.NOT_FOUND).build();

        maintenanceMapper.delete(maintenance_uuid);

        return Response.noContent().build();
    }
}
