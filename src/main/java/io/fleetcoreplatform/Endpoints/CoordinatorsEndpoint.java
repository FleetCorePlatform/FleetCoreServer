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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@NoCache
@Path("/api/v1/coordinators")
//@RolesAllowed("${allowed.superadmin.role-name}")
@Tag(name = "Coordinators", description = "Operations related to coordinator management")
public class CoordinatorsEndpoint {

    @Inject
    CoordinatorMapper coordinatorMapper;

    @Inject
    CoreService coreService;

    @GET
    @Path("/{coordinator_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    @Operation(summary = "Get coordinator", description = "Get details of a specific coordinator")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DbCoordinator.class))),
        @APIResponse(responseCode = "404", description = "Coordinator not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getCoordinator(
        @Parameter(description = "UUID of the coordinator", required = true)
        @PathParam("coordinator_uuid") UUID coordinator_uuid
    ) {
        try {
            DbCoordinator coordinator = coordinatorMapper.findByUuid(
                coordinator_uuid
            );

            if (coordinator == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(coordinator).build();
        } catch (Exception e) {
            return Response.status(
                Response.Status.INTERNAL_SERVER_ERROR
            ).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RateLimit(value = 2, window = 1, windowUnit = ChronoUnit.HOURS)
    @Operation(summary = "Register coordinator", description = "Register a new coordinator")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Coordinator registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, description = "Temporary password"))),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response registerCoordinator(
        @RequestBody(description = "Coordinator registration details", required = true)
        CoordinatorRequestModel coordinatorRequestModel
    ) {
        if (
            coordinatorRequestModel == null ||
            coordinatorRequestModel.email() == null ||
            coordinatorRequestModel.firstName() == null ||
            coordinatorRequestModel.lastName() == null
        ) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            CognitoCreatedResponseModel response =
                coreService.registerNewCoordinator(
                    coordinatorRequestModel.email(),
                    coordinatorRequestModel.firstName(),
                    coordinatorRequestModel.lastName()
                );

            if (response == null) {
                return Response.status(
                    Response.Status.INTERNAL_SERVER_ERROR
                ).build();
            }

            return Response.ok(response.temp_password()).build();
        } catch (Exception ex) {
            return Response.status(
                Response.Status.INTERNAL_SERVER_ERROR
            ).build();
        }
    }

    @PATCH
    @Path("/{coordinator_uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    @Operation(summary = "Update coordinator", description = "Update details of an existing coordinator")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Coordinator updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request body"),
        @APIResponse(responseCode = "404", description = "Coordinator not found"),
        @APIResponse(responseCode = "500", description = "Internal server error")
    })
    public Response updateCoordinator(
        @Parameter(description = "UUID of the coordinator", required = true)
        @PathParam("coordinator_uuid") UUID coordinator_uuid,
        @RequestBody(description = "Update details", required = true)
        UpdateCoordinatorModel body
    ) {
        if (
            body == null ||
            (body.lastName() == null && body.firstName() == null)
        ) {
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
