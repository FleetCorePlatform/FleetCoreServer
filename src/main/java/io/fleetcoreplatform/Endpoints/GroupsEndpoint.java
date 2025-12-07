package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Exceptions.GroupNotEmptyException;
import io.fleetcoreplatform.Managers.Database.DbModels.DbGroup;
import io.fleetcoreplatform.Managers.Database.DbModels.DbOutpost;
import io.fleetcoreplatform.Managers.Database.Mappers.GroupMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.OutpostMapper;
import io.fleetcoreplatform.Models.GroupRequestModel;
import io.fleetcoreplatform.Models.UpdateGroupOutpostModel;
import io.fleetcoreplatform.Services.CoreService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

@NoCache
@Path("/api/v1/groups/")
// @RolesAllowed("${allowed.role-name}")
public class GroupsEndpoint {
    @Inject GroupMapper groupMapper;
    @Inject OutpostMapper outpostMapper;
    @Inject CoreService coreService;
    Logger logger = Logger.getLogger(GroupsEndpoint.class.getName());

    @POST
    @Path("/create/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    public Response createGroup(GroupRequestModel group) {
        if (group == null || group.outpost_uuid() == null || group.group_name() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        DbGroup checkExists = groupMapper.findByName(group.group_name());
        if (checkExists != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        try {
            DbOutpost outpost = outpostMapper.findByUuid(group.outpost_uuid());

            if (outpost == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            coreService.createNewGroup(group.group_name(), group.outpost_uuid());

            return Response.status(Response.Status.CREATED).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/delete/{group_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    public Response deleteGroup(@PathParam("group_uuid") UUID group_uuid) {
        try {
            DbGroup group = groupMapper.findByUuid(group_uuid);

            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            coreService.tryDeleteGroup(group.getName());

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (GroupNotEmptyException gne) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    @Path("/update/{group_uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    public Response updateGroup(
            @PathParam("group_uuid") UUID group_uuid, UpdateGroupOutpostModel body) {
        if (body == null || body.outpost_uuid() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            coreService.updateGroup(group_uuid, body);

            return Response.noContent().build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
