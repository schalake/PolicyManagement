package com.eana.insurance.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.*;
import software.amazon.awssdk.services.sns.model.*;

@Service
public class SQSPublishMessage {

    private static final Logger logger = LoggerFactory.getLogger(SQSPublishMessage.class);

    @Value("${aws.topic.name}")
    private String topicName;

    private final SnsClient snsClient;

    @Autowired
    public SQSPublishMessage(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    public void publishRequestToSNS(String requestJSONString) throws Exception {
        logger.info("Starting publishing message to SNS...");
        // 1. Check if the topic exists
        ListTopicsResponse listTopicsResponse = snsClient.listTopics(ListTopicsRequest.builder().build());
        String topicArn = null;

        // Find the topic ARN by checking if the topic exists in the list of topics
        for (Topic topic : listTopicsResponse.topics()) {
            if (topic.topicArn().endsWith(topicName)) {
                topicArn = topic.topicArn();
                logger.debug("SNS topic found: {}", topicArn);
                break;
            }
        }

        if (topicArn == null) {
            logger.error("SNS topic '{}' not found", topicName);
            throw new Exception("SNS topic not found");
        }

        // 2. Publish a Message to SNS
        snsClient.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .message(requestJSONString)
                .build());

        //System.out.println("Published message to SNS: " + requestJSONString);
        logger.info("Published message to SNS: {}", requestJSONString);
    }
}