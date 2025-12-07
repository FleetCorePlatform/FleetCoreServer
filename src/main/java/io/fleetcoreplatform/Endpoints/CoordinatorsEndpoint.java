package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.Database.DbModels.DbCoordinator;
import io.fleetcoreplatform.Managers.Database.Mappers.CoordinatorMapper;
import io.fleetcoreplatform.Models.CognitoCreatedResponseModel;
import io.fleetcoreplatform.Models.CoordinatorRequestModel;
import io.fleetcoreplatform.Models.UpdateCoordinatorModel;
import io.fleetcoreplatform.Services.CoreService;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.jboss.resteasy.reactive.NoCache;

@NoCache
@Path("/api/v1/coordinators/")
//@RolesAllowed("${allowed.superadmin.role-name}")
public class CoordinatorsEndpoint {
    @Inject CoordinatorMapper coordinatorMapper;
    @Inject CoreService coreService;

    @GET
    @Path("/{coordinator_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    public Response getCoordinator(@PathParam("coordinator_uuid") UUID coordinator_uuid) {
        try {
            DbCoordinator coordinator = coordinatorMapper.findByUuid(coordinator_uuid);

            if (coordinator == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(coordinator).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/register/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RateLimit(value = 2, window = 1, windowUnit = ChronoUnit.HOURS)
    public Response registerCoordinator(CoordinatorRequestModel coordinatorRequestModel) {
        if (coordinatorRequestModel == null
                || coordinatorRequestModel.email() == null
                || coordinatorRequestModel.firstName() == null
                || coordinatorRequestModel.lastName() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            CognitoCreatedResponseModel response =
                    coreService.registerNewCoordinator(
                            coordinatorRequestModel.email(),
                            coordinatorRequestModel.firstName(),
                            coordinatorRequestModel.lastName());

            if (response == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

            return Response.ok(response.temp_password()).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    @Path("/update/{coordinator_uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    public Response updateCoordinator(
            @PathParam("coordinator_uuid") UUID coordinator_uuid, UpdateCoordinatorModel body) {
        if (body == null || (body.lastName() == null && body.firstName() == null)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            coreService.updateCoordinator(coordinator_uuid, body);

            return Response.noContent().build();
        } catch (NotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception ex) {
            return Response.status(500, ex.getMessage()).build();
        }
    }
}
