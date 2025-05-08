package com.eana.insurance.aws;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sqs.*;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sns.*;
import software.amazon.awssdk.services.sns.model.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AWSLocalStackServices {

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

    @PostConstruct
    public void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("test", "test");
        this.creds = StaticCredentialsProvider.create(credentials);
        this.localstack = URI.create(localstackEndpoint);
        this.region = Region.of(regionValue);
    }

    public void publishToSNSANDSaveToDynamo(String requestJSONString) throws Exception {
        var sns = SnsClient.builder().credentialsProvider(creds).endpointOverride(localstack).region(region).build();
        var sqs = SqsClient.builder().credentialsProvider(creds).endpointOverride(localstack).region(region).build();


        // 1. Create Topic
        String topicArn = sns.createTopic(CreateTopicRequest.builder().name(topicName).build()).topicArn();

        // 2. Create Queue
        String queueUrl = sqs.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();

        // 3. Get Queue ARN
        String queueArn = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN).build())
                .attributes().get(QueueAttributeName.QUEUE_ARN);

        // 4. Subscribe SQS to SNS
        ListSubscriptionsByTopicResponse subs = sns.listSubscriptionsByTopic(
                ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build());

        boolean alreadySubscribed = subs.subscriptions().stream()
                .anyMatch(sub -> sub.endpoint().equals(queueArn) && sub.protocol().equals("sqs"));

        if (!alreadySubscribed) {
            Map<QueueAttributeName, String> attributes = Map.of(
                    QueueAttributeName.POLICY, createSQSPolicy(queueArn, topicArn)
            );
            sqs.setQueueAttributes(SetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributes(attributes)
                    .build());

            sns.subscribe(SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("sqs")
                    .endpoint(queueArn)
                    .build());
        }

        // 5. Publish a Message to SNS
        sns.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .message(requestJSONString)
                .build());

        System.out.println("Publish message from SNS: " + requestJSONString);

        // 6. Receive Message from SQS
        List<Message> messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(2)
                .build()).messages();

        // 7. Put item in DynamoDB
        if (!messages.isEmpty()) {
            String msg = messages.get(0).body();
            System.out.println("Received from SQS: " + msg);

            DynamoDbClient dynamo = DynamoDbClient.builder().credentialsProvider(creds).endpointOverride(localstack).region(region).build();
            // Check if table exists and create if it doesn't
            if (!doesTableExist(dynamo, dynamoTable)) {
                createTable(dynamo, dynamoTable);
            } else {
                System.out.println("Table " + dynamoTable + " already exists.");
            }

            String uuid = UUID.randomUUID().toString();
            dynamo.putItem(PutItemRequest.builder()
                    .tableName(dynamoTable)
                    .item(Map.of(
                            "id", AttributeValue.builder().s(uuid).build(),
                            "Message", AttributeValue.builder().s(msg).build()
                    ))
                    .build());

            System.out.println("Message saved to DynamoDB with id: " + uuid);

            //8. Delete message from SQS to prevent reprocessing
            sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(messages.get(0).receiptHandle())
                    .build());

            System.out.println("Message deleted from SQS.");
        }
    }

    private static String createSQSPolicy(String queueArn, String topicArn) {
        return """
                {
                    "Version":"2012-10-17",
                    "Statement":[{
                        "Effect":"Allow",
                        "Principal":"*",
                        "Action":"sqs:SendMessage",
                        "Resource":"%s",
                        "Condition":{
                            "ArnEquals":{
                                "aws:SourceArn":"%s"
                            }
                        }
                    }]
                }
                """.formatted(queueArn, topicArn);
    }

    // Method to check if the table exists
    private static boolean doesTableExist(DynamoDbClient dynamoDbClient, String tableName) {
        try {
            // List tables and check if the table exists in the list
            ListTablesResponse listTablesResponse = dynamoDbClient.listTables();
            return listTablesResponse.tableNames().contains(tableName);
        } catch (SdkClientException e) {
            System.out.println("Error listing tables: " + e.getMessage());
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
            System.out.println("Table " + tableName + " created successfully.");
        } catch (SdkClientException e) {
            System.out.println("Error creating table: " + e.getMessage());
        }
    }
}