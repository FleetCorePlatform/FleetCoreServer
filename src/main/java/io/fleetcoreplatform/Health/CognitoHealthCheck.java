package io.fleetcoreplatform.Health;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import io.fleetcoreplatform.Managers.Cognito.CognitoManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Objects;

@Readiness
@ApplicationScoped
public class CognitoHealthCheck implements HealthCheck {

    @Inject CognitoManager cognitoClient;
    @Inject ApplicationConfig config;

    @Override
    public HealthCheckResponse call() {
        try {
            var res = cognitoClient.getClient().listUserPools(ListUserPoolsRequest.builder()
                    .maxResults(1)
                    .build()).join();

            if (!Objects.equals(res.userPools().getFirst().id(), config.cognito().userPoolId())) {
                throw new Error("Cannot find configured cognito user pool");
            }

            return HealthCheckResponse.up("AWS Cognito connection");

        } catch (Exception e) {
            return HealthCheckResponse.named("AWS Cognito connection")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}