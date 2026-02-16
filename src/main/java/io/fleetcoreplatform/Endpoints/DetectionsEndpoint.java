package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.Database.DbModels.DbDetection;
import io.fleetcoreplatform.Managers.Database.DbModels.DbMission;
import io.fleetcoreplatform.Managers.Database.Mappers.DetectionsMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.MissionMapper;
import io.fleetcoreplatform.Models.DetectionValidationRequestModel;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/detections")
public class DetectionsEndpoint {
    @Inject DetectionsMapper detectionsMapper;
    @Inject MissionMapper missionMapper;
    @Inject SecurityIdentity identity;
    @Inject Logger logger;

    @GET
    public Response getDetections(@QueryParam("group_uuid") UUID groupUuid, @QueryParam("mission_uuid") UUID missionUuid) {
        if (missionUuid == null || groupUuid == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            String cognitoSub = identity.getPrincipal().getName();

            DbMission mission = missionMapper.findByIdAndCoordinator(missionUuid, cognitoSub);
            if (mission == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            List<DbDetection> detections = detectionsMapper.listByMissionGroupAndCoordinator(missionUuid, groupUuid, cognitoSub);
            return Response.ok(detections).build();

        } catch (Exception e) {
            logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    public Response validateDetection(@QueryParam("mission_uuid") UUID missionUuid, @QueryParam("detection_uuid") UUID detectionUuid, @RequestBody DetectionValidationRequestModel body) {
        if (missionUuid == null || detectionUuid == null || body == null || body.false_positive() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            String cognitoSub = identity.getPrincipal().getName();

            DbDetection detection = detectionsMapper.findByUuidAndCoordinator(detectionUuid, missionUuid, cognitoSub);
            if (detection == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            int rowsAffected = detectionsMapper.validateDetection(detectionUuid, body.false_positive(), cognitoSub);

            if (rowsAffected == 0) {
                return Response.notModified().build();
            }

            return Response.noContent().build();

        } catch (Exception e) {
            logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
