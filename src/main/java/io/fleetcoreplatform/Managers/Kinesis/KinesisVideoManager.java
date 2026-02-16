package io.fleetcoreplatform.Managers.Kinesis;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoAsyncClient;
import software.amazon.awssdk.services.kinesisvideo.model.*;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class KinesisVideoManager {
    @Inject ApplicationConfig config;
    @Inject
    Logger logger;

    private KinesisVideoAsyncClient kinesisAsyncClient;

    @PostConstruct
    void init() {
        SdkAsyncHttpClient asyncHttpClient =
                NettyNioAsyncHttpClient.builder()
                        .maxConcurrency(100)
                        .connectionTimeout(Duration.ofSeconds(60))
                        .readTimeout(Duration.ofSeconds(60))
                        .writeTimeout(Duration.ofSeconds(60))
                        .build();

        kinesisAsyncClient =
                KinesisVideoAsyncClient.builder()
                        .region(Region.of(config.kinesis().region()))
                        .httpClient(asyncHttpClient)
                        .build();
    }

    @PreDestroy
    void destroy() {
        kinesisAsyncClient.close();
    }

    public UUID createSignalingChannel(UUID droneUuid) {
        CreateSignalingChannelRequest request = CreateSignalingChannelRequest
                .builder()
                .channelName(droneUuid.toString())
                .build();

        try {
            CompletableFuture<CreateSignalingChannelResponse> future = kinesisAsyncClient.createSignalingChannel(request);
            var response = future.join();

            if (response.sdkHttpResponse().isSuccessful()) {
                return droneUuid;
            } else {
                return null;
            }

        } catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    public void deleteSignalingChannel(UUID droneUuid) {
        try {
            DescribeSignalingChannelRequest describeRequest = DescribeSignalingChannelRequest
                    .builder()
                    .channelName(droneUuid.toString())
                    .build();

            CompletableFuture<DescribeSignalingChannelResponse> describeFuture = kinesisAsyncClient.describeSignalingChannel(describeRequest);
            DescribeSignalingChannelResponse describeResponse = describeFuture.join();

            String channelArn = describeResponse.channelInfo().channelARN();

            DeleteSignalingChannelRequest deleteRequest = DeleteSignalingChannelRequest
                    .builder()
                    .channelARN(channelArn)
                    .build();

            CompletableFuture<DeleteSignalingChannelResponse> deleteFuture = kinesisAsyncClient.deleteSignalingChannel(deleteRequest);
            DeleteSignalingChannelResponse deleteResponse = deleteFuture.join();

            if (deleteResponse.sdkHttpResponse().isSuccessful()) {
                logger.info(String.format("Successfully deleted signaling channel for drone: %s", droneUuid));
            } else {
                logger.error(String.format("Failed to delete signaling channel for drone: %s", droneUuid));
            }

        } catch (Exception e) {
            logger.error("Error deleting signaling channel for drone: " + droneUuid, e);
        }
    }
}
