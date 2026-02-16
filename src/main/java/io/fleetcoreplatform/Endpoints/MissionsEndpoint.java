package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.Database.DbModels.DbCoordinator;
import io.fleetcoreplatform.Managers.Database.DbModels.DbMission;
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
    public Response cancelMission(
            @PathParam("mission_uuid") UUID missionUUID,
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
    public Response getAllMissionSummaries(@QueryParam("group_uuid") UUID groupUuid) {
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
