package io.fleetcoreplatform.Configs;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "aws")
public interface ApplicationConfig {
    @WithName("region")
    String region();

    @WithName("s3")
    S3Config s3();

    @WithName("iot")
    IoTCoreConfig iot();

    @WithName("cognito")
    CognitoConfig cognito();

    interface S3Config {
        @WithName("bucket-name")
        String bucketName();
    }

    interface IoTCoreConfig {
        @WithName("thing-type")
        String thingType();

        @WithName("pubsub-client-id")
        String pubsubClientId();

        @WithName("mission-cancel-job-arn")
        String missionCancelJobArn();

        @WithName("new-mission-job-arn")
        String newMissionJobArn();
    }

    interface CognitoConfig {
        @WithName("user-pool-id")
        String userPoolId();
    }
}
