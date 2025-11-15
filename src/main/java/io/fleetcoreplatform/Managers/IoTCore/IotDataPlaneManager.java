package io.fleetcoreplatform.Managers.IoTCore;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.IotDataPlaneException;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;
import software.amazon.awssdk.services.iotdataplane.model.PublishResponse;

@ApplicationScoped
public class IotDataPlaneManager {
    private IotDataPlaneClient iotDataPlaneClient;

    @Inject ApplicationConfig config;

    @PostConstruct
    public void init() {
        iotDataPlaneClient =
                IotDataPlaneClient.builder().region(Region.of(config.region())).build();
    }

    @PreDestroy
    public void destroy() {
        iotDataPlaneClient.close();
    }

    public void publish(String topic, String payload) {
        String jsonPayload = Json.createObjectBuilder().add("message", payload).build().toString();

        PublishRequest publishRequest =
                PublishRequest.builder()
                        .topic(topic)
                        .payload(SdkBytes.fromString(jsonPayload, StandardCharsets.UTF_8))
                        .qos(0)
                        .contentType("application/json")
                        .build();

        try {
            PublishResponse response = iotDataPlaneClient.publish(publishRequest);
            if (response.sdkHttpResponse().isSuccessful()) {
                System.out.println("Successfully published to " + topic);
            } else {
                System.out.println("Failed to publish to " + topic);
            }
        } catch (Exception e) {
            if (e instanceof IotDataPlaneException) {
                System.out.println(((IotDataPlaneException) e).awsErrorDetails().errorMessage());
            } else {
                System.out.println(e.getMessage());
            }
        }
    }
}
