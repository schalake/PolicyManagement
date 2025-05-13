#!/bin/bash
set -e

echo "setup-sns-sqs.sh script is running..."

TOPIC_NAME="PolicyTopic"
QUEUE_NAME="PolicyQueue"
LOCALSTACK_ENDPOINT="http://localhost:4566"

echo "Creating SQS queue..."
QUEUE_URL=$(aws sqs create-queue \
  --queue-name "$QUEUE_NAME" \
  --endpoint-url="$LOCALSTACK_ENDPOINT" \
  --query 'QueueUrl' \
  --output text)

echo "Creating SNS topic..."
TOPIC_ARN=$(aws sns create-topic \
  --name "$TOPIC_NAME" \
  --endpoint-url="$LOCALSTACK_ENDPOINT" \
  --query 'TopicArn' \
  --output text)

echo "Getting Queue ARN..."
QUEUE_ARN=$(aws sqs get-queue-attributes \
  --queue-url "$QUEUE_URL" \
  --attribute-names QueueArn \
  --endpoint-url="$LOCALSTACK_ENDPOINT" \
  --query 'Attributes.QueueArn' \
  --output text)

echo "Setting Queue Policy to allow SNS to publish..."
POLICY="'{
  \"Version\": \"2012-10-17\",
  \"Statement\": [
    {
      \"Sid\": \"Allow-SNS-SendMessage\",
      \"Effect\": \"Allow\",
      \"Principal\": \"*\",
      \"Action\": \"SQS:SendMessage\",
      \"Resource\": \"$QUEUE_ARN\",
      \"Condition\": {
        \"ArnEquals\": {
          \"aws:SourceArn\": \"$TOPIC_ARN\"
        }
      }
    }
  ]
}'"

# Apply the policy
aws sqs set-queue-attributes \
  --queue-url "$QUEUE_URL" \
  --attributes "Policy=$POLICY" \
  --endpoint-url="$LOCALSTACK_ENDPOINT"

echo "Subscribing queue to SNS topic..."
aws sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$QUEUE_ARN" \
  --endpoint-url="$LOCALSTACK_ENDPOINT"

echo "SNS and SQS setup complete."
