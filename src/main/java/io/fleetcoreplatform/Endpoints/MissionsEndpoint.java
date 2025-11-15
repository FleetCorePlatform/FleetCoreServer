package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.SQS.QueueManager;
import io.fleetcoreplatform.Models.CreateMissionRequestModel;
import io.fleetcoreplatform.Models.DroneExecutionStatusResponseModel;
import io.fleetcoreplatform.Services.CoreService;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.iot.model.IotException;

@Path("/api/v1/missions")
// @RolesAllowed("${allowed.role-name}")
public class MissionsEndpoint {
    @Inject CoreService coreService;
    @Inject QueueManager sqsManager;
    @Inject Logger logger;

    @POST
    @Path("/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 2, window = 10, windowUnit = ChronoUnit.MINUTES)
    public Response startOutpostSurvey(CreateMissionRequestModel body) {
        if (body == null
                || body.outpost() == null
                || body.group() == null
                || body.coordinatorUUID() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            coreService.createNewMission(body.outpost(), body.group(), body.coordinatorUUID());

            return Response.ok().build();
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

    @GET
    @Path("/status/{drone_uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMissionStatus(@PathParam("drone_uuid") UUID droneUUID)
            throws NotFoundException {
        try {
            DroneExecutionStatusResponseModel status = coreService.getMissionStatus(droneUUID);

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
