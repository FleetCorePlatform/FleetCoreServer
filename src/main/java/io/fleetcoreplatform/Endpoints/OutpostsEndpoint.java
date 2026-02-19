package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Exceptions.OutpostNotEmptyException;
import io.fleetcoreplatform.Managers.Database.DbModels.DbGroup;
import io.fleetcoreplatform.Managers.Database.DbModels.DbOutpost;
import io.fleetcoreplatform.Managers.Database.Mappers.CoordinatorMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.GroupMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.OutpostMapper;
import io.fleetcoreplatform.Models.CreateOutpostModel;
import io.fleetcoreplatform.Models.OutpostGeofenceUpdateRequest;
import io.fleetcoreplatform.Models.OutpostGroupSummary;
import io.fleetcoreplatform.Models.OutpostSummary;
import io.fleetcoreplatform.Services.CoreService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

// TODO: Implement row-level authentication on outposts endpoint (#28)

@Path("/api/v1/outposts")
// @RolesAllowed("${allowed.role-name}")
@Tag(name = "Outposts", description = "Operations related to outpost management")
public class OutpostsEndpoint {
    @Inject OutpostMapper outpostMapper;
    @Inject CoordinatorMapper coordinatorMapper;
    @Inject GroupMapper groupMapper;
    @Inject SecurityIdentity identity;
    @Inject Logger logger;
    @Inject CoreService coreService;

    @GET
    @Path("/{outpost_uuid}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    @Operation(summary = "List groups for an outpost", description = "Retrieves all groups associated with a specific outpost")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DbGroup.class, type = SchemaType.ARRAY))),
        @APIResponse(responseCode = "404", description = "Not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getGroupsByOutpost(
        @Parameter(description = "UUID of the outpost")
        @PathParam("outpost_uuid") UUID outpostUuid
    ) {
        try {
            String cognitoSub = identity.getPrincipal().getName();
            List<DbGroup> groups = groupMapper.listGroupsByOutpostUuidAndCoordinator(outpostUuid, cognitoSub);

            if (groups == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(groups).build();

        } catch (Exception e) {
            logger.error("Error while listing groups for outpost", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    // TODO: Implement OutpostsEndpoint (#24)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    @Operation(summary = "Create outpost", description = "Create a new outpost")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Outpost created successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response createOutpost(
            @RequestBody(description = "Outpost creation details", required = true)
            CreateOutpostModel body) {
        if (body == null
                || body.name() == null
                || body.latitude() == null
                || body.longitude() == null
                || body.area() == null
                || body.area().points.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String cognitoSub = identity.getPrincipal().getName();
        var coordinator = coordinatorMapper.findByCognitoSub(cognitoSub);

        if (coordinator == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        UUID uuid = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(Instant.now());

        String areaWkt =
                "POLYGON(("
                        + body.area().points.stream()
                                .map(p -> p.x() + " " + p.y())
                                .collect(Collectors.joining(","))
                        + "))";

        try {
            outpostMapper.insert(
                    uuid,
                    body.name(),
                    body.longitude(),
                    body.latitude(),
                    areaWkt,
                    coordinator.getUuid(),
                    timestamp);

            return Response.created(URI.create(String.format("/api/v1/outposts/%s", uuid))).build();
        } catch (Exception e) {
            logger.error("Error while inserting outpost", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List outposts", description = "List all outposts for the coordinator")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.ARRAY, implementation = DbOutpost.class))),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response listOutposts() {
        try {
            String cognitoSub = identity.getPrincipal().getName();
            List<DbOutpost> outposts = outpostMapper.listByCoordinator(cognitoSub);

            return Response.ok(outposts).build();
        } catch (Exception e) {
            logger.error("Error while listing outposts", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{outpost-uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get outpost", description = "Get details of a specific outpost")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DbOutpost.class))),
        @APIResponse(responseCode = "400", description = "Bad request"),
        @APIResponse(responseCode = "404", description = "Outpost not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getOutpost(
            @Parameter(description = "UUID of the outpost", required = true)
            @PathParam("outpost-uuid") String outpostUUID) {
        try {
            String cognitoSub = identity.getPrincipal().getName();
            UUID uuid = UUID.fromString(outpostUUID);

            DbOutpost outpost = outpostMapper.findByUuidAndCoordinator(uuid, cognitoSub);

            if (outpost == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(outpost).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error while fetching outpost", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/{outpost-uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Edit outpost geofence", description = "Update the geofence area of an outpost")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Geofence updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Outpost not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response editOutpostGeofence(
        @Parameter(description = "UUID of the outpost", required = true)
        @PathParam("outpost-uuid") UUID outpostUuid,
        @RequestBody(description = "Geofence update details", required = true)
        OutpostGeofenceUpdateRequest body
    ) {
        if (outpostUuid == null || body == null || body.area() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String cognitoSub = identity.getPrincipal().getName();

        try {

            DbOutpost outpost = outpostMapper.findByUuidAndCoordinator(outpostUuid, cognitoSub);
            if (outpost == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            outpostMapper.updateArea(outpostUuid, body.area());

            return Response.noContent().build();

        } catch (Exception e) {
            logger.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{outpost-uuid}/summary")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get outpost summary", description = "Get summary of an outpost including groups")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OutpostSummary.class))),
        @APIResponse(responseCode = "404", description = "Outpost not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getOutpostSummary(
            @Parameter(description = "UUID of the outpost", required = true)
            @PathParam("outpost-uuid") UUID outpostUUID) {
        try {
            String cognitoSub = identity.getPrincipal().getName();

            DbOutpost outpost = outpostMapper.findByUuidAndCoordinator(outpostUUID, cognitoSub);
            if (outpost == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            List<OutpostGroupSummary> groupSummaries = outpostMapper.findGroupsByOutpostAndCoordinator(outpostUUID, cognitoSub);
            OutpostSummary summary = new OutpostSummary(outpost.getName(), outpost.getUuid(), outpost.getLatitude(), outpost.getLongitude(), outpost.getCreated_at(), groupSummaries, outpost.getArea());

            return Response.ok(summary).build();
        } catch (Exception e) {
            logger.error("Error fetching outpost summary", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{outpost-uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete outpost", description = "Permanently delete an outpost")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Outpost deleted successfully"),
        @APIResponse(responseCode = "304", description = "Not modified (Outpost not empty)"),
        @APIResponse(responseCode = "404", description = "Outpost not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response tryDeleteOutpost(
            @Parameter(description = "UUID of the outpost", required = true)
            @PathParam("outpost-uuid") UUID outpostUUID) {
        try {
            String cognitoSub = identity.getPrincipal().getName();

            DbOutpost outpost = outpostMapper.findByUuidAndCoordinator(outpostUUID, cognitoSub);
            if (outpost == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            coreService.deleteOutpost(outpostUUID);

            return Response.noContent().build();
        } catch (OutpostNotEmptyException one) {
            return Response.notModified(one.getMessage()).build();
        } catch (Exception e) {
            logger.error("Error fetching outpost summary", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
