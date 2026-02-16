package io.fleetcoreplatform.Health;

import io.fleetcoreplatform.Managers.IoTCore.IotManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class KinesisHealthCheck implements HealthCheck {

    @Inject KinesisVideoClient kinesisClient;

    @Override
    public HealthCheckResponse call() {
        try {
            kinesisClient.getClient();
            // TODO: Implement kinesis video health check

            return HealthCheckResponse.up("AWS Kinesis Video connection");
        } catch (Exception e) {
            return HealthCheckResponse.named("AWS Kinesis Video connection")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}