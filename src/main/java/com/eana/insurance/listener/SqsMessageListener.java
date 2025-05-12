package com.eana.insurance.listener;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SqsMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(SqsMessageListener.class);

    private final SqsClient sqsClient;
    //private final String queueUrl = "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/PolicyQueue"; // replace with actual URL

    @Value("${aws.region:us-east-1}")
    private String regionValue;

    @Value("${aws.localstack.endpoint}")
    private String localstackEndpoint;

    @Value("${aws.accesskey}")
    private String accessKey;

    @Value("${aws.secretkey}")
    private String secretKey;

    @Value("${aws.topic.name}")
    private String topicName;

    @Value("${aws.queue.name}")
    private String queueName;

    @Value("${aws.dynamo.table}")
    private String dynamoTable;

    private StaticCredentialsProvider creds;
    private URI localstack;
    private Region region;

    @PostConstruct
    public void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        this.creds = StaticCredentialsProvider.create(credentials);
        this.localstack = URI.create(localstackEndpoint);
        this.region = Region.of(regionValue);
        logger.info("Initialized region: {}, endpoint: {}", regionValue, localstackEndpoint);
    }

    @Autowired
    public SqsMessageListener(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Scheduled(fixedDelay = 1000)
    public void pollMessages() {
        logger.info("Polling SQS for messages...");
        // Queue URL
        String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();
        logger.debug("SQS queue found: {}", queueUrl);
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(5)
                .waitTimeSeconds(1)
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();

        for (Message message : messages) {
            System.out.println("Received: " + message.body());

            // process message...
            // Put item in DynamoDB
            if (!messages.isEmpty()) {
                String msg = messages.get(0).body();
                //System.out.println("Received from SQS: " + msg);
                logger.info("Received message from SQS: {}", msg);

                DynamoDbClient dynamo = DynamoDbClient.builder().credentialsProvider(creds).endpointOverride(localstack).region(region).build();
                // Check if table exists and create if it doesn't
                if (!doesTableExist(dynamo, dynamoTable)) {
                    createTable(dynamo, dynamoTable);
                } else {
                    //System.out.println("Table " + dynamoTable + " already exists.");
                    logger.info("Table '{}' already exists.", dynamoTable);
                }

                String uuid = UUID.randomUUID().toString();
                dynamo.putItem(PutItemRequest.builder()
                        .tableName(dynamoTable)
                        .item(Map.of(
                                "id", AttributeValue.builder().s(uuid).build(),
                                "Message", AttributeValue.builder().s(msg).build()
                        ))
                        .build());

                //System.out.println("Message saved to DynamoDB with id: " + uuid);
                logger.info("Saved message to DynamoDB with id: {}", uuid);
                //Delete message from SQS to prevent reprocessing
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());

                //System.out.println("Deleted message from SQS.");
                logger.info("Deleted message from SQS.");
            } else {
                logger.warn("No messages received from SQS.");
            }

            // delete after processing
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        }
    }

    // Method to check if the table exists
    private static boolean doesTableExist(DynamoDbClient dynamoDbClient, String tableName) {
        try {
            // List tables and check if the table exists in the list
            ListTablesResponse listTablesResponse = dynamoDbClient.listTables();
            return listTablesResponse.tableNames().contains(tableName);
        } catch (SdkClientException e) {
            //System.out.println("Error listing tables: " + e.getMessage());
            logger.error("Error listing DynamoDB tables: {}", e.getMessage());
            return false;
        }
    }

    // Method to create the table if it doesn't exist
    private static void createTable(DynamoDbClient dynamoDbClient, String tableName) {
        try {
            // Create table request
            CreateTableRequest createTableRequest = CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH) // Partition key
                            .build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.S) // String type for 'id'
                            .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build();

            // Create the table
            dynamoDbClient.createTable(createTableRequest);
            //System.out.println("Table " + tableName + " created successfully.");
            logger.info("Created DynamoDB table '{}'.", tableName);
        } catch (SdkClientException e) {
            //System.out.println("Error creating DynamoDB table: " + e.getMessage());
            logger.error("Error creating DynamoDB table '{}': {}", tableName, e.getMessage());
        }
    }
}