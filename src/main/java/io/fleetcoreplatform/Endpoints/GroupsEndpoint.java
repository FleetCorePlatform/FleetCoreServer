package io.fleetcoreplatform.Endpoints;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import io.fleetcoreplatform.Exceptions.GroupNotEmptyException;
import io.fleetcoreplatform.Managers.Database.DbModels.DbGroup;
import io.fleetcoreplatform.Managers.Database.DbModels.DbOutpost;
import io.fleetcoreplatform.Managers.Database.Mappers.GroupMapper;
import io.fleetcoreplatform.Managers.Database.Mappers.OutpostMapper;
import io.fleetcoreplatform.Managers.SQS.SqsManager;
import io.fleetcoreplatform.Models.DroneSummaryModel;
import io.fleetcoreplatform.Models.DroneTelemetryModel;
import io.fleetcoreplatform.Models.GroupRequestModel;
import io.fleetcoreplatform.Models.UpdateGroupOutpostModel;
import io.fleetcoreplatform.Services.CoreService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

@NoCache
@Path("/api/v1/groups")
public class GroupsEndpoint {
    @Inject GroupMapper groupMapper;
    @Inject OutpostMapper outpostMapper;
    @Inject CoreService coreService;
    @Inject SecurityIdentity identity;
    @Inject
    SqsManager sqsManager;

    @Inject Logger logger;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    public Response getGroups(
        @Nullable @QueryParam("outpost_uuid") UUID outpostUuid,
        @Nullable @QueryParam("group_uuid") UUID groupUuid,
        @DefaultValue("10") @QueryParam("limit") Integer limit,
        @Context ApplicationConfig config
    ) {
        try {
            String cognitoSub = identity.getPrincipal().getName();

            if (outpostUuid != null && groupUuid == null) {
                List<DbGroup> groups = groupMapper.listGroupsByOutpostUuidAndCoordinator(outpostUuid, cognitoSub);
                if (groups == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                return Response.ok(groups).build();
            }

            if (groupUuid != null && outpostUuid == null) {
                List<DroneSummaryModel> drones = groupMapper.listDronesByGroupAndCoordinator(groupUuid, cognitoSub, limit);
                if (drones == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }

                List<DroneTelemetryModel> telemetryModels = sqsManager.ingestQueue(config.sqs().queueName());

                Map<String, DroneTelemetryModel> telemetryByDevice = telemetryModels.stream()
                    .collect(Collectors.toMap(
                        DroneTelemetryModel::device_name,
                        Function.identity(),
                        (existing, replacement) -> replacement
                    ));

                List<DroneSummaryModel> dronesWithTelemetry = drones.stream()
                    .map(drone -> {
                        DroneTelemetryModel telem = telemetryByDevice.get(drone.getName());
                        if (telem != null) {
                            return new DroneSummaryModel(
                                drone.getUuid(),
                                drone.getName(),
                                drone.getGroup_name(),
                                drone.getAddress(),
                                drone.getManager_version(),
                                drone.getFirst_discovered(),
                                drone.getHome_position(),
                                drone.getMaintenance(),
                                telem.battery().remaining_percent(),
                                true,
                                drone.getSignaling_channel_name()
                            );
                        }
                        return new DroneSummaryModel(
                            drone.getUuid(),
                            drone.getName(),
                            drone.getGroup_name(),
                            drone.getAddress(),
                            drone.getManager_version(),
                            drone.getFirst_discovered(),
                            drone.getHome_position(),
                            drone.getMaintenance(),
                            null,
                            false,
                            drone.getSignaling_channel_name()
                        );
                    })
                    .toList();

                return Response.ok(dronesWithTelemetry).build();
            }

            return Response.status(Response.Status.BAD_REQUEST).build();

        } catch (Exception e) {
            logger.error("Error while listing groups", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{group_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    public Response getGroup(@PathParam("group_uuid") UUID group_uuid) {
        try {
            DbGroup group = groupMapper.findByUuid(group_uuid);
            if (group == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(group).build();
        } catch (Exception e) {
            logger.error("Error fetching group details", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
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
            logger.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{group_uuid}")
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
            logger.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    @Path("/{group_uuid}")
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
            logger.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
