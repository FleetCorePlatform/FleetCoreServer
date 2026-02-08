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
public class IotHealthCheck implements HealthCheck {

    @Inject IotManager iotClient;

    @Override
    public HealthCheckResponse call() {
        try {
            iotClient.getClient().describeEndpoint(
                DescribeEndpointRequest.builder()
                    .endpointType("iot:Data-ATS")
                    .build()
            );

            return HealthCheckResponse.up("AWS IoT Core connection");
        } catch (Exception e) {
            return HealthCheckResponse.named("AWS IoT Core connection")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}