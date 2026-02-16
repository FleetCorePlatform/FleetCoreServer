package io.fleetcoreplatform.Health;

import io.fleetcoreplatform.Managers.Kinesis.KinesisVideoManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.kinesisvideo.model.ListStreamsRequest;

@Readiness
@ApplicationScoped
public class KinesisHealthCheck implements HealthCheck {

    @Inject KinesisVideoManager kinesisClient;

    @Override
    public HealthCheckResponse call() {
        try {
            kinesisClient.getClient().listStreams(
                ListStreamsRequest.builder().maxResults(1).build()
            );

            return HealthCheckResponse.up("AWS Kinesis Video connection");
        } catch (Exception e) {
            return HealthCheckResponse.named("AWS Kinesis Video connection")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}