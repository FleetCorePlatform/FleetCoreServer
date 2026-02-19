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
import org.jboss.logging.Logger;

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

@Path("/api/v1/detections")
@Tag(name = "Detections", description = "Operations related to drone detections")
public class DetectionsEndpoint {
    @Inject DetectionsMapper detectionsMapper;
    @Inject MissionMapper missionMapper;
    @Inject SecurityIdentity identity;
    @Inject Logger logger;

    @GET
    @Operation(summary = "Get detections", description = "List detections for a mission and group")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.ARRAY, implementation = DbDetection.class))),
        @APIResponse(responseCode = "400", description = "Bad request"),
        @APIResponse(responseCode = "404", description = "Mission not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getDetections(
            @Parameter(description = "UUID of the group", required = true)
            @QueryParam("group_uuid") UUID groupUuid,
            @Parameter(description = "UUID of the mission", required = true)
            @QueryParam("mission_uuid") UUID missionUuid) {
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
    @Operation(summary = "Validate detection", description = "Mark a detection as valid or false positive")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Detection validated successfully"),
        @APIResponse(responseCode = "304", description = "Not modified"),
        @APIResponse(responseCode = "400", description = "Bad request"),
        @APIResponse(responseCode = "404", description = "Detection not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response validateDetection(
            @Parameter(description = "UUID of the mission", required = true)
            @QueryParam("mission_uuid") UUID missionUuid,
            @Parameter(description = "UUID of the detection", required = true)
            @QueryParam("detection_uuid") UUID detectionUuid,
            @RequestBody(description = "Validation details", required = true)
            DetectionValidationRequestModel body) {
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
