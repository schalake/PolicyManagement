package com.eana.insurance.aws;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.*;
import software.amazon.awssdk.services.sns.model.*;
import java.net.URI;

@Service
public class AWSPublishMessage {

    private static final Logger logger = LoggerFactory.getLogger(AWSPublishMessage.class);

    @Value("${aws.region:us-east-1}")
    private String regionValue;

    @Value("${aws.localstack.endpoint}")
    private String localstackEndpoint;

    @Value("${aws.topic.name}")
    private String topicName;

    @Value("${aws.queue.name}")
    private String queueName;

    @Value("${aws.dynamo.table}")
    private String dynamoTable;

    private StaticCredentialsProvider creds;
    private URI localstack;
    private Region region;

    private final SnsClient snsClient;

    @Autowired
    public AWSPublishMessage(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @PostConstruct
    public void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("test", "test");
        this.creds = StaticCredentialsProvider.create(credentials);
        this.localstack = URI.create(localstackEndpoint);
        this.region = Region.of(regionValue);
        logger.info("Initialized AWSPublishMessage with region: {}, endpoint: {}", regionValue, localstackEndpoint);
    }

    public void publishRequestToSNS(String requestJSONString) throws Exception {
        logger.info("Starting SNS message publishing...");
        // 1. Retrieve Topic
        String topicArn = snsClient.createTopic(CreateTopicRequest.builder().name(topicName).build()).topicArn();
        logger.debug("SNS topic found: {}", topicArn);

        // 2. Publish a Message to SNS
        snsClient.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .message(requestJSONString)
                .build());

        //System.out.println("Published message to SNS: " + requestJSONString);
        logger.info("Published message to SNS: {}", requestJSONString);
    }
}