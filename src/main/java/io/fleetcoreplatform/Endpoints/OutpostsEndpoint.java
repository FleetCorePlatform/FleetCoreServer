package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Managers.Database.DbModels.DbOutpost;
import io.fleetcoreplatform.Managers.Database.Mappers.CoordinatorMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.OutpostMapper;
import io.fleetcoreplatform.Models.CreateOutpostModel;
import io.fleetcoreplatform.Models.OutpostGroupSummary;
import io.fleetcoreplatform.Models.OutpostSummary;
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
import org.jboss.logging.Logger;

// TODO: Implement row-level authentication on outposts endpoint (#28)

@Path("/api/v1/outposts")
// @RolesAllowed("${allowed.role-name}")
public class OutpostsEndpoint {
    @Inject OutpostMapper outpostMapper;
    @Inject CoordinatorMapper coordinatorMapper;
    @Inject SecurityIdentity identity;
    @Inject Logger logger;

    // TODO: Implement OutpostsEndpoint (#24)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    public Response createOutpost(CreateOutpostModel body) {
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
    public Response getOutpost(@PathParam("outpost-uuid") String outpostUUID) {
        try {
            String cognitoSub = identity.getPrincipal().getName();
            UUID uuid = UUID.fromString(outpostUUID);

            DbOutpost outpost = outpostMapper.findByUuidAndCoordinator(uuid, cognitoSub);

            if (outpost == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(outpost).build();
        } catch (IllegalArgumentException e) {
            // Catches invalid UUID strings
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error while fetching outpost", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{outpost-uuid}/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOutpostSummary(@PathParam("outpost-uuid") UUID outpostUUID) {
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
}
