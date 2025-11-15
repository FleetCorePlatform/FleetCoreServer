package io.fleetcoreplatform.Managers.S3;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ApplicationScoped
public class StorageManager {
    S3Client s3Client;

    @Inject ApplicationConfig config;

    private Logger logger;

    @PostConstruct
    void init() {
        logger = Logger.getLogger(StorageManager.class.getName());

        s3Client =
                S3Client.builder()
                        .region(Region.of(config.region()))
                        .credentialsProvider(DefaultCredentialsProvider.builder().build())
                        .build();
    }

    @PreDestroy
    void destroy() {
        s3Client.close();
    }

    public boolean createBucket(String bucketName) {
        try {
            if (bucketExists(bucketName)) {
                logger.info("Bucket " + bucketName + " already exists");
                return true;
            }

            CreateBucketRequest createBucketRequest =
                    CreateBucketRequest.builder().bucket(bucketName).build();

            CreateBucketResponse response = s3Client.createBucket(createBucketRequest);
            logger.info("Bucket " + bucketName + " created successfully");
            return true;

        } catch (S3Exception e) {
            logger.severe("Failed to create bucket " + bucketName + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.severe("Unexpected error creating bucket " + bucketName + ": " + e.getMessage());
            return false;
        }
    }

    private boolean bucketExists(String bucketName) {
        try {
            HeadBucketRequest headBucketRequest =
                    HeadBucketRequest.builder().bucket(bucketName).build();

            s3Client.headBucket(headBucketRequest);
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    public String uploadMissionBundle(String bundleUUID, InputStream missionBundle)
            throws IOException {
        byte[] bytes = missionBundle.readAllBytes();
        String bundleKey = bundleUUID + ".bundle.zip";

        try {
            PutObjectResponse response =
                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(config.s3().bucketName())
                                    .key(bundleKey)
                                    .contentType("zip")
                                    .build(),
                            RequestBody.fromInputStream(
                                    new ByteArrayInputStream(bytes), bytes.length));

            if (response != null && response.sdkHttpResponse().isSuccessful()) {
                return bundleKey;
            } else {
                return null;
            }

        } catch (Exception e) {
            if (e instanceof S3Exception) {
                logger.log(Level.SEVERE, ((S3Exception) e).awsErrorDetails().errorMessage(), e);
            } else {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * Uploads an object to AWS S3
     *
     * @param bundlePath The full name of the object, can use <b>/</b> for folders
     * @param missionBundle The actual file to upload to the bucket
     * @return The key of the created S3 object
     */
    public String uploadMissionBundle(String bundlePath, File missionBundle) {
        String bundleKey = bundlePath + ".bundle.zip";

        try {
            PutObjectResponse response =
                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(config.s3().bucketName())
                                    .key(bundleKey)
                                    .contentType("application/zip")
                                    .build(),
                            missionBundle.toPath());

            if (response != null && response.sdkHttpResponse().isSuccessful()) {
                if (!missionBundle.delete()) {
                    logger.warning("Cleanup failed, manual tmp cleaning required!");
                }
                ;
                return bundleKey;
            } else {
                return null;
            }

        } catch (Exception e) {
            if (e instanceof S3Exception) {
                logger.log(Level.SEVERE, ((S3Exception) e).awsErrorDetails().errorMessage(), e);
            } else {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
            return null;
        }
    }

    public String getInternalObjectUrl(String key) {
        return "s3://" + config.s3().bucketName() + "/" + key;
    }

    public String getPresignedObjectUrl(String key) {
        try (S3Presigner presigner = S3Presigner.create()) {
            GetObjectRequest getObjectRequest =
                    GetObjectRequest.builder().bucket(config.s3().bucketName()).key(key).build();

            GetObjectPresignRequest presignRequest =
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(20))
                            .getObjectRequest(getObjectRequest)
                            .build();

            PresignedGetObjectRequest presignedGetObjectRequest =
                    presigner.presignGetObject(presignRequest);

            return presignedGetObjectRequest.url().toExternalForm();
        }
    }
}
