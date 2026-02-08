package io.fleetcoreplatform.Managers.SQS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import io.fleetcoreplatform.Configs.ApplicationConfig;
import io.fleetcoreplatform.Models.DroneTelemetryModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@ApplicationScoped
public class SqsManager {
    private SqsClient sqsClient;
    private ObjectMapper mapper;

    @Inject ApplicationConfig config;

    @PostConstruct
    public void init() {
        sqsClient = SqsClient.builder().region(Region.of(config.region())).build();
        mapper = new CBORMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    @PreDestroy
    public void destroy() {
        sqsClient.close();
    }

    public SqsClient getClient() {
        return sqsClient;
    }

    public List<DroneTelemetryModel> ingestQueue(String queueName) {
        GetQueueUrlRequest getQueueUrlRequest =
                GetQueueUrlRequest.builder().queueName(queueName).build();

        String queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();

        ReceiveMessageRequest receiveRequest =
                ReceiveMessageRequest.builder().maxNumberOfMessages(10).queueUrl(queueUrl).build();

        ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveRequest);

        List<DroneTelemetryModel> result = new ArrayList<>();

        receiveMessageResponse
                .messages()
                .forEach(
                        message -> {
                            try {
                                String messageBody = message.body();

                                byte[] cborBytes = Base64.getDecoder().decode(messageBody);

                                DroneTelemetryModel model =
                                        mapper.readValue(cborBytes, DroneTelemetryModel.class);
                                result.add(model);

                                DeleteMessageRequest deleteRequest =
                                        DeleteMessageRequest.builder()
                                                .queueUrl(queueUrl)
                                                .receiptHandle(message.receiptHandle())
                                                .build();

                                sqsClient.deleteMessage(deleteRequest);
                            } catch (IOException e) {
                                System.err.printf("Failed to deserialize CBOR message: %s%n", e);
                            }
                        });

        return result;
    }
}
