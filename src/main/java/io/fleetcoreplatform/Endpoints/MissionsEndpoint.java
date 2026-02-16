package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.Database.DbModels.DbCoordinator;
import io.fleetcoreplatform.Managers.Database.Mappers.CoordinatorMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.MissionMapper;
import io.fleetcoreplatform.Managers.SQS.SqsManager;
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
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.iot.model.IotException;

@Path("/api/v1/missions")
@RolesAllowed("${allowed.role-name}")
public class MissionsEndpoint {
    @Inject CoreService coreService;
    @Inject
    SqsManager sqsManager;
    @Inject MissionMapper missionMapper;
    @Inject SecurityIdentity identity;

    @Inject CoordinatorMapper coordinatorMapper;
    @Inject Logger logger;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 2, window = 10, windowUnit = ChronoUnit.MINUTES)
    public Response startOutpostSurvey(CreateMissionRequestModel body) {
        if (body == null
                || body.outpost() == null
                || body.groupUUID() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        DbCoordinator coordinator = coordinatorMapper.findByCognitoSub(identity.getAttribute("sub"));
        if (coordinator == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            UUID missionUUID =
                    coreService.createNewMission(
                            body.outpost(),
                            body.groupUUID(),
                            coordinator.getUuid(),
                            body.altitude());

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
    public Response cancelMission(@PathParam("mission_uuid") UUID missionUUID, @RequestBody CancelMissionBodyModel body) {
        // TODO: Implement mission cancel logic (#17)
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMissionSummariesForGroup(@QueryParam("group_uuid") UUID groupUuid) {
        String cognitoSub = identity.getPrincipal().getName();

        try {
            List<MissionSummary> missions = missionMapper.selectMissionSummariesByGroupAndCoordinator(groupUuid, cognitoSub);
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMissionSummariesForCoordinator(@QueryParam("count") Integer count) {
        if (count == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        try {
            List<MissionSummary> missions = missionMapper.selectLatestMissionSummariesByCoordinator(cognitoSub, count);
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
    @Path("/{mission_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMissionStatus(@PathParam("mission_uuid") UUID missionUUID)
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
    public Response getThingMissionStatus(@PathParam("mission_uuid") UUID missionUUID, @PathParam("drone_uuid") UUID droneUUID)
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
