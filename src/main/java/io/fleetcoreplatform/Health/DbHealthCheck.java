package io.fleetcoreplatform.Health;

import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Readiness
@ApplicationScoped
public class DbHealthCheck implements HealthCheck {

    @Inject DataSource dataSource;

    @Override
    public HealthCheckResponse call() {

        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Database connection");

        try {
            verifyDatabaseConnection();
            responseBuilder.up();
        } catch (SQLException e) {
            responseBuilder.down()
                           .withData("error", e.getMessage());
        }

        return responseBuilder.build();
    }

    private void verifyDatabaseConnection() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(2)) {
                throw new SQLException("Connection is not valid");
            }
        }
    }
}