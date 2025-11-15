package io.fleetcoreplatform.Exceptions;

import io.smallrye.faulttolerance.api.RateLimitException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RateLimitExceptionMapper implements ExceptionMapper<RateLimitException> {
    @Override
    public Response toResponse(RateLimitException exception) {
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity("Rate limit exceeded!")
                .build();
    }
}
