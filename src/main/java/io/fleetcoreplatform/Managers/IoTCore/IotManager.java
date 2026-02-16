package io.fleetcoreplatform.Managers.IoTCore;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import io.fleetcoreplatform.Managers.IoTCore.Enums.MissionDocumentEnums;
import io.fleetcoreplatform.Models.DroneStatusModel;
import io.fleetcoreplatform.Models.IoTCertContainer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.IotAsyncClient;
import software.amazon.awssdk.services.iot.model.*;
import software.amazon.awssdk.services.sts.StsClient;

@ApplicationScoped
public class IotManager {
    @Inject ApplicationConfig config;

    private IotAsyncClient iotAsyncClient;
    private String accountIdentifier;

    @PostConstruct
    void init() {
        SdkAsyncHttpClient asyncHttpClient =
                NettyNioAsyncHttpClient.builder()
                        .maxConcurrency(100)
                        .connectionTimeout(Duration.ofSeconds(60))
                        .readTimeout(Duration.ofSeconds(60))
                        .writeTimeout(Duration.ofSeconds(60))
                        .build();

        iotAsyncClient =
                IotAsyncClient.builder()
                        .region(Region.of(config.region()))
                        .httpClient(asyncHttpClient)
                        .build();

        try (StsClient stsClient = StsClient.create()) {
            accountIdentifier = stsClient.getCallerIdentity().account();
        }
    }

    public IotAsyncClient getClient() {
        return iotAsyncClient;
    }

    public String createPolicy(String thingName) throws CompletionException {
        String policyName = thingName + "-policy";
        String policyDocument =
                IotDocumentBuilder.buildPolicyDocument(accountIdentifier, config.region(), config.iot().roleAlias());

        CreatePolicyRequest createPolicyRequest =
                CreatePolicyRequest.builder()
                        .policyName(policyName)
                        .policyDocument(policyDocument)
                        .build();

        CompletableFuture<CreatePolicyResponse> future =
                iotAsyncClient.createPolicy(createPolicyRequest);

        CreatePolicyResponse createPolicyResponse = future.join();
        return createPolicyResponse.policyName();
    }

    public void attachPolicyToCertificate(String certificateARN, String policyARN) {
        AttachPolicyRequest attachPolicyRequest =
                AttachPolicyRequest.builder().policyName(policyARN).target(certificateARN).build();

        AttachPolicyResponse attachPolicyResponse =
                iotAsyncClient.attachPolicy(attachPolicyRequest).join();
    }

    public JobExecutionSummary getThingJob(String thingName, String jobId) {
        ListJobExecutionsForThingRequest listJobExecutionsForThingRequest =
                ListJobExecutionsForThingRequest.builder().jobId(jobId).thingName(thingName).build();

        CompletableFuture<ListJobExecutionsForThingResponse> future =
                iotAsyncClient.listJobExecutionsForThing(listJobExecutionsForThingRequest);

        try {
            ListJobExecutionsForThingResponse response = future.join();

            return response.executionSummaries().getFirst().jobExecutionSummary();
        } catch (Exception e) {
            return null;
        }
    }

    public Job getJob(String jobId) {
        DescribeJobRequest request =
            DescribeJobRequest.builder().jobId(jobId).build();

        CompletableFuture<DescribeJobResponse> future =
                iotAsyncClient.describeJob(request);
        try {
            DescribeJobResponse response = future.join();

            return response.job();
        } catch (Exception e) {
            return null;
        }
    }

    public void addDeviceToGroup(String thingName, String groupARN) {
        AddThingToThingGroupRequest addThingToThingGroupRequest =
                AddThingToThingGroupRequest.builder()
                        .thingName(thingName)
                        .thingGroupArn(groupARN)
                        .build();

        CompletableFuture<AddThingToThingGroupResponse> future =
                iotAsyncClient.addThingToThingGroup(addThingToThingGroupRequest);
        future.whenComplete(
                (response, throwable) -> {
                    if (throwable == null && response.sdkHttpResponse().isSuccessful()) {
                        System.out.printf(
                                "Successfully added %S device to group %s%n", thingName, groupARN);
                    } else {
                        System.out.printf("Failed to add device to group %s%n", thingName);
                    }
                });
    }

    public String getGroupARN(String groupName) {
        DescribeThingGroupRequest describeThingGroupRequest =
                DescribeThingGroupRequest.builder().thingGroupName(groupName).build();

        CompletableFuture<DescribeThingGroupResponse> future =
                iotAsyncClient.describeThingGroup(describeThingGroupRequest);
        DescribeThingGroupResponse response = future.join();

        return response.thingGroupArn();
    }

    public IoTCertContainer generateCertificate() {
        CompletableFuture<CreateKeysAndCertificateResponse> future =
                iotAsyncClient.createKeysAndCertificate();
        CreateKeysAndCertificateResponse response = future.join();

        return new IoTCertContainer(
                response.keyPair().privateKey(),
                response.certificatePem(),
                response.certificateArn());
    }

    public void attachCertificate(String deviceName, String certificateARN) {
        AttachThingPrincipalRequest attachThingPrincipalRequest =
                AttachThingPrincipalRequest.builder()
                        .thingName(deviceName)
                        .principal(certificateARN)
                        .build();

        CompletableFuture<AttachThingPrincipalResponse> future =
                iotAsyncClient.attachThingPrincipal(attachThingPrincipalRequest);
        future.whenComplete(
                (attachThingPrincipalResponse, ex) -> {
                    if (ex != null
                            && attachThingPrincipalResponse.sdkHttpResponse().isSuccessful()) {
                        System.out.printf(
                                "Successfully attached certificate with ARN %s to device %s%n",
                                certificateARN, deviceName);
                    } else if (ex instanceof IotException) {
                        System.err.println(((IotException) ex).awsErrorDetails().errorMessage());
                    } else {
                        System.err.println(ex.getMessage());
                    }
                });

        future.join();
    }

    public void createThing(String thingName, String outpost, String group) {
        AttributePayload attributePayload = AttributePayload.builder()
            .attributes(Map.of(
                "outpost", outpost,
                "group", group
            ))
            .build();

        CreateThingRequest createThingRequest =
                CreateThingRequest.builder()
                        .thingName(thingName)
                        .thingTypeName(config.iot().thingType())
                        .attributePayload(attributePayload)
                        .build();

        CompletableFuture<CreateThingResponse> future =
                iotAsyncClient.createThing(createThingRequest);
        future.whenComplete(
                (createThingResponse, ex) -> {
                    if (createThingResponse != null
                            && createThingResponse.sdkHttpResponse().isSuccessful()) {
                        System.out.println(
                                thingName
                                        + " was successfully created. The ARN value is "
                                        + createThingResponse.thingArn());
                    } else {
                        Throwable cause = ex.getCause();
                        if (cause instanceof IotException) {
                            System.err.println(
                                    ((IotException) cause).awsErrorDetails().errorMessage());
                        } else {
                            System.err.println("Unexpected error: " + cause.getMessage());
                        }
                    }
                });

        future.join();
    }

    public String getThingGroup(String thingName) {
        ListThingGroupsForThingRequest listThingGroupsForThingRequest =
                ListThingGroupsForThingRequest.builder().thingName(thingName).maxResults(1).build();

        ListThingGroupsForThingResponse future =
                iotAsyncClient.listThingGroupsForThing(listThingGroupsForThingRequest).join();
        return future.thingGroups().getFirst().groupArn();
    }

    public void removeThing(String thingName) {
        DeleteThingRequest deleteThingRequest =
                DeleteThingRequest.builder().thingName(thingName).build();

        CompletableFuture<DeleteThingResponse> future =
                iotAsyncClient.deleteThing(deleteThingRequest);

        DeleteThingResponse response = future.join();
    }

    public void removeThingFromGroup(String thingName, String groupARN) {
        RemoveThingFromThingGroupRequest removeThingFromThingGroupRequest =
                RemoveThingFromThingGroupRequest.builder()
                        .thingName(thingName)
                        .thingGroupArn(groupARN)
                        .build();

        iotAsyncClient.removeThingFromThingGroup(removeThingFromThingGroupRequest).join();
    }

    public void removePolicies(String principalARN) {
        ListAttachedPoliciesRequest listAttachedPoliciesRequest =
                ListAttachedPoliciesRequest.builder().target(principalARN).build();

        ListAttachedPoliciesResponse response =
                iotAsyncClient.listAttachedPolicies(listAttachedPoliciesRequest).join();

        response.policies()
                .forEach(
                        policy -> {
                            DetachPolicyRequest detachRequest =
                                    DetachPolicyRequest.builder()
                                            .policyName(policy.policyName())
                                            .target(principalARN)
                                            .build();
                            iotAsyncClient.detachPolicy(detachRequest).join();

                            DeletePolicyRequest deleteRequest =
                                    DeletePolicyRequest.builder()
                                            .policyName(policy.policyName())
                                            .build();
                            iotAsyncClient.deletePolicy(deleteRequest).join();
                        });
    }

    public void deleteCertificates(String deviceName) {
        ListThingPrincipalsRequest listThingPrincipalsRequest =
                ListThingPrincipalsRequest.builder().thingName(deviceName).build();

        CompletableFuture<ListThingPrincipalsResponse> future =
                iotAsyncClient.listThingPrincipals(listThingPrincipalsRequest);

        ListThingPrincipalsResponse response = future.join();
        response.principals().stream()
                .toList()
                .forEach(
                        principalARN -> {
                            String certificateId =
                                    principalARN.substring(principalARN.lastIndexOf('/') + 1);

                            UpdateCertificateRequest updateRequest =
                                    UpdateCertificateRequest.builder()
                                            .certificateId(certificateId)
                                            .newStatus(CertificateStatus.INACTIVE)
                                            .build();
                            iotAsyncClient.updateCertificate(updateRequest).join();

                            DeleteCertificateRequest deleteRequest =
                                    DeleteCertificateRequest.builder()
                                            .certificateId(certificateId)
                                            .forceDelete(true)
                                            .build();
                            iotAsyncClient.deleteCertificate(deleteRequest).join();
                        });
    }

    public void detachCertificates(String deviceName) {
        ListThingPrincipalsRequest listThingPrincipalsRequest =
                ListThingPrincipalsRequest.builder().thingName(deviceName).build();

        CompletableFuture<ListThingPrincipalsResponse> future =
                iotAsyncClient.listThingPrincipals(listThingPrincipalsRequest);

        ListThingPrincipalsResponse response = future.join();
        List<String> principalARNs = new ArrayList<>(response.principals());

        principalARNs.forEach(
                arn -> {
                    removePolicies(arn);

                    DetachThingPrincipalRequest detachThingPrincipalRequest =
                            DetachThingPrincipalRequest.builder()
                                    .principal(arn)
                                    .thingName(deviceName)
                                    .build();

                    iotAsyncClient.detachThingPrincipal(detachThingPrincipalRequest).join();
                });
    }

    public void createThingGroup(String groupName, String outpostName) {
        CreateThingGroupRequest createThingGroupRequest =
                CreateThingGroupRequest.builder()
                        .thingGroupProperties(
                                ThingGroupProperties.builder()
                                        .attributePayload(
                                                AttributePayload.builder()
                                                        .attributes(Map.of("outpost", outpostName))
                                                        .build())
                                        .build())
                        .thingGroupName(groupName)
                        .build();

        CompletableFuture<CreateThingGroupResponse> future =
                iotAsyncClient.createThingGroup(createThingGroupRequest);
        future.whenComplete(
                (createThingGroupResponse, ex) -> {
                    if (createThingGroupResponse != null
                            && createThingGroupResponse.sdkHttpResponse().isSuccessful()) {
                        System.out.println("Successfully created group " + groupName);
                    } else if (ex instanceof IotException) {
                        System.err.println(((IotException) ex).awsErrorDetails().errorMessage());
                    }
                });

        future.join();
    }

    public void removeThingGroup(String groupName) {
        DeleteThingGroupRequest deleteGroupRequest =
                DeleteThingGroupRequest.builder().thingGroupName(groupName).build();

        CompletableFuture<DeleteThingGroupResponse> future =
                iotAsyncClient.deleteThingGroup(deleteGroupRequest);
        future.whenComplete(
                (res, ex) -> {
                    if (ex != null) {
                        if (ex instanceof IotException) {
                            System.err.println(
                                    ((IotException) ex).awsErrorDetails().errorMessage());
                        }
                    }
                    if (res != null && res.sdkHttpResponse().isSuccessful()) {
                        System.out.println("Successfully removed group " + groupName);
                    }
                });
    }

    public void updateThingGroupOutpost(String groupName, String newOutpostName) {
        UpdateThingGroupRequest updateThingGroupRequest =
                UpdateThingGroupRequest.builder()
                        .thingGroupName(groupName)
                        .thingGroupProperties(
                                ThingGroupProperties.builder()
                                        .attributePayload(
                                                AttributePayload.builder()
                                                        .attributes(
                                                                Map.of("outpost", newOutpostName))
                                                        .build())
                                        .build())
                        .build();

        CompletableFuture<UpdateThingGroupResponse> future =
                iotAsyncClient.updateThingGroup(updateThingGroupRequest);
        future.whenComplete(
                (res, ex) -> {
                    if (ex != null) {
                        if (ex instanceof IotException) {
                            System.err.println(
                                    ((IotException) ex).awsErrorDetails().errorMessage());
                        }
                    }
                    if (res != null && res.sdkHttpResponse().isSuccessful()) {
                        System.out.println("Successfully updated group " + groupName);
                    }
                });

        future.join();
    }

    public void createIoTJob(
            String groupARN,
            MissionDocumentEnums action,
            String jobName,
            String downloadUrl,
            String filePath,
            String outpost,
            String group,
            String bucket) {
        String jobDocument = IotDocumentBuilder.buildJobDocument(action, jobName, downloadUrl, filePath, outpost, group, bucket);

        CreateJobRequest createJobRequest =
                CreateJobRequest.builder()
                        .jobId(jobName)
                        .targets(groupARN)
                        .targetSelection(TargetSelection.SNAPSHOT)
                        .document(jobDocument)
                        .build();

        CompletableFuture<CreateJobResponse> future = iotAsyncClient.createJob(createJobRequest);
        future.whenComplete(
                (jobResponse, ex) -> {
                    if (jobResponse != null && jobResponse.sdkHttpResponse().isSuccessful()) {
                        System.out.println("New job was successfully created.");
                    } else {
                        Throwable cause = ex.getCause();
                        if (cause instanceof IotException) {
                            System.err.println(
                                    ((IotException) cause).awsErrorDetails().errorMessage());
                        } else {
                            System.err.println("Unexpected error: " + cause.getMessage());
                        }
                    }
                });

        future.join();
    }

    public void deleteDevice(String deviceName) {
        DeleteThingRequest deleteThingRequest =
                DeleteThingRequest.builder().thingName(deviceName).build();

        CompletableFuture<DeleteThingResponse> future =
                iotAsyncClient.deleteThing(deleteThingRequest);
        future.whenComplete(
                (deleteThingResponse, ex) -> {
                    if (deleteThingResponse != null) {
                        System.out.println(deviceName + " was successfully deleted.");
                    } else {
                        Throwable cause = ex.getCause();
                        if (cause instanceof IotException) {
                            System.err.println(
                                    ((IotException) cause).awsErrorDetails().errorMessage());
                        }
                    }
                });

        future.join();
    }

    public void describeDevice(String deviceName) {
        DescribeThingRequest thingRequest =
                DescribeThingRequest.builder().thingName(deviceName).build();

        CompletableFuture<DescribeThingResponse> future =
                iotAsyncClient.describeThing(thingRequest);
        future.whenComplete(
                (describeResponse, ex) -> {
                    if (describeResponse != null) {
                        System.out.println("Thing Details:");
                        System.out.println("Thing Name: " + describeResponse.thingName());
                        System.out.println("Thing ARN: " + describeResponse.thingArn());
                    } else {
                        Throwable cause = ex != null ? ex.getCause() : null;
                        if (cause instanceof IotException) {
                            System.err.println(
                                    ((IotException) cause).awsErrorDetails().errorMessage());
                        } else if (cause != null) {
                            System.err.println("Unexpected error: " + cause.getMessage());
                        } else {
                            System.err.println("Failed to describe Thing.");
                        }
                    }
                });

        future.join();
    }

    public Map<String, String> getGroupAttributes(String groupName) {
        DescribeThingGroupRequest thingRequest =
                DescribeThingGroupRequest.builder().thingGroupName(groupName).build();

        try {
            CompletableFuture<DescribeThingGroupResponse> future =
                    iotAsyncClient.describeThingGroup(thingRequest);

            DescribeThingGroupResponse describeResponse = future.join();

            var attributes = describeResponse.thingGroupProperties().attributePayload().attributes();

            if (attributes != null && !attributes.isEmpty()) {
                return attributes;
            } else {
                return null;
            }

        } catch (Exception ex) {
            if (ex instanceof IotException) {
                System.err.println(
                        ((IotException) ex).awsErrorDetails().errorMessage());
            } else {
                System.err.println("Unexpected error: " + ex.getMessage());
            }

            return null;
        }
    }

    public DroneStatusModel getDroneStatus(String deviceName) {
        SearchIndexRequest request = SearchIndexRequest.builder()
                .indexName("AWS_Things")
                .queryString("thingName:\"" + deviceName + "\"")
                .build();

        SearchIndexResponse indexResponse = iotAsyncClient.searchIndex(request).join();

        if (indexResponse.hasThings() && !indexResponse.things().isEmpty()) {
            ThingDocument thing = indexResponse.things().getFirst();

            if (thing.connectivity() != null) {
                return new DroneStatusModel(thing.connectivity().timestamp(), thing.connectivity().connected());
            }

            return new DroneStatusModel(null, false);
        }
        return null;
    }
}
