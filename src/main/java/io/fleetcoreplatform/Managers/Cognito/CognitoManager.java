package io.fleetcoreplatform.Managers.Cognito;

import io.fleetcoreplatform.Configs.ApplicationConfig;
import io.fleetcoreplatform.Models.CognitoCreatedResponseModel;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

@Startup
@ApplicationScoped
public class CognitoManager {
    @Inject ApplicationConfig config;

    private Logger logger;

    private CognitoIdentityProviderAsyncClient client;

    @PostConstruct
    public void init() {
        logger = Logger.getLogger(CognitoManager.class.getName());

        client =
                CognitoIdentityProviderAsyncClient.builder()
                        .region(Region.of(config.region()))
                        .build();
    }

    @PreDestroy
    public void destroy() {
        client.close();
    }

    public CognitoCreatedResponseModel createUser(String email, String firstName, String lastName)
            throws SdkException {
        AttributeType emailAttribute = AttributeType.builder().name("email").value(email).build();

        AttributeType firstNameAttribute =
                AttributeType.builder().name("given_name").value(firstName).build();

        AttributeType lastNameAttribute =
                AttributeType.builder().name("family_name").value(lastName).build();

        List<AttributeType> attributes =
                Arrays.asList(emailAttribute, firstNameAttribute, lastNameAttribute);

        String tempPassword = UUID.randomUUID().toString().replace("-", "").concat("A%");

        AdminCreateUserRequest adminCreateUserRequest =
                AdminCreateUserRequest.builder()
                        .userAttributes(attributes)
                        .userPoolId(config.cognito().userPoolId())
                        .username(email)
                        .messageAction(MessageActionType.SUPPRESS)
                        .temporaryPassword(tempPassword)
                        .build();

        CompletableFuture<AdminCreateUserResponse> future =
                client.adminCreateUser(adminCreateUserRequest);

        try {
            AdminCreateUserResponse response = future.join();

            if (response.sdkHttpResponse().isSuccessful()) {
                return new CognitoCreatedResponseModel(
                        tempPassword, response.user().attributes().getLast().value());
            } else {
                logger.warning("Failed to create user: " + response.sdkHttpResponse().statusText());
                return null;
            }

        } catch (Exception e) {
            logger.severe("Failed to create user: " + e.getMessage());
            return null;
        }
    }

    public void updateUser(String email, String firstName, String lastName) throws SdkException {
        List<AttributeType> attributes = new ArrayList<>();

        if (firstName != null) {
            logger.info("Setting first name: " + firstName);
            attributes.add(AttributeType.builder().name("given_name").value(firstName).build());
        }
        if (lastName != null) {
            logger.info("Setting last name: " + lastName);
            attributes.add(AttributeType.builder().name("family_name").value(lastName).build());
        }

        AdminUpdateUserAttributesRequest request =
                AdminUpdateUserAttributesRequest.builder()
                        .userPoolId(config.cognito().userPoolId())
                        .username(email)
                        .userAttributes(attributes)
                        .build();

        CompletableFuture<AdminUpdateUserAttributesResponse> future =
                client.adminUpdateUserAttributes(request);

        try {
            AdminUpdateUserAttributesResponse response = future.join();

            if (response.sdkHttpResponse().isSuccessful()) {
                logger.info(
                        "Successfully updated user: " + response.sdkHttpResponse().statusText());
            } else {
                logger.warning("Failed to update user: " + response.sdkHttpResponse().statusText());
            }
        } catch (Exception e) {
            logger.severe("Failed to update user: " + e.getMessage());
        }
    }

    public void removeUser(String email) throws SdkException {
        AdminDeleteUserRequest adminDeleteUserRequest =
                AdminDeleteUserRequest.builder()
                        .userPoolId(config.cognito().userPoolId())
                        .username(email)
                        .build();

        CompletableFuture<AdminDeleteUserResponse> future =
                client.adminDeleteUser(adminDeleteUserRequest);

        try {
            AdminDeleteUserResponse response = future.join();

            if (response.sdkHttpResponse().isSuccessful()) {
                logger.info("Successfully removed cognito user " + email);
            }

        } catch (SdkException e) {
            logger.severe("Failed to remove cognito user: " + e.getMessage());
            throw e;
        } catch (Exception ex) {
            logger.severe("Failed to remove cognito user: " + ex.getMessage());
        }
    }
}
