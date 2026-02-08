package io.fleetcoreplatform.Health;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import io.fleetcoreplatform.Managers.SQS.SqsManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Readiness
@ApplicationScoped
public class SqsHealthCheck implements HealthCheck {

    @Inject SqsManager sqsClient;
    @Inject ApplicationConfig config;

    @Override
    public HealthCheckResponse call() {
        try {
            sqsClient.getClient().listQueues(ListQueuesRequest.builder().queueNamePrefix(config.sqs().queueName()).maxResults(1).build());

            return HealthCheckResponse.up("AWS SQS connection");
        } catch (Exception e) {
            return HealthCheckResponse.named("AWS SQS connection")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}