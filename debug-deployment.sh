#!/bin/bash

echo "🔍 HealthApp Deployment Debug Script"
echo "====================================="

# Set variables
CLUSTER_NAME="healthapp-cluster"
SERVICE_NAME="healthapp-service"
REGION="us-east-1"
ALB_NAME="healthapp-alb"

echo ""
echo "1. Checking ECS Service Status..."
echo "--------------------------------"
aws ecs describe-services \
  --cluster $CLUSTER_NAME \
  --services $SERVICE_NAME \
  --region $REGION \
  --query 'services[0].{status:status,runningCount:runningCount,desiredCount:desiredCount,pendingCount:pendingCount}' \
  --output table

echo ""
echo "2. Checking Recent ECS Tasks..."
echo "-------------------------------"
aws ecs list-tasks \
  --cluster $CLUSTER_NAME \
  --region $REGION \
  --output table

echo ""
echo "3. Checking Recent Service Events..."
echo "-----------------------------------"
aws ecs describe-services \
  --cluster $CLUSTER_NAME \
  --services $SERVICE_NAME \
  --region $REGION \
  --query 'services[0].events[0:5]' \
  --output table

echo ""
echo "4. Checking Load Balancer Status..."
echo "----------------------------------"
aws elbv2 describe-load-balancers \
  --names $ALB_NAME \
  --region $REGION \
  --query 'LoadBalancers[0].{DNSName:DNSName,State:State.Code}' \
  --output table

echo ""
echo "5. Checking Target Group Health..."
echo "---------------------------------"
TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups \
  --names healthapp-tg \
  --region $REGION \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

aws elbv2 describe-target-health \
  --target-group-arn $TARGET_GROUP_ARN \
  --region $REGION \
  --output table

echo ""
echo "6. Checking CloudWatch Log Groups..."
echo "-----------------------------------"
aws logs describe-log-groups \
  --log-group-name-prefix "/ecs/healthapp" \
  --region $REGION \
  --query 'logGroups[].{logGroupName:logGroupName,storedBytes:storedBytes}' \
  --output table

echo ""
echo "7. Testing Health Endpoint..."
echo "----------------------------"
ALB_DNS=$(aws elbv2 describe-load-balancers \
  --names $ALB_NAME \
  --region $REGION \
  --query 'LoadBalancers[0].DNSName' \
  --output text)

echo "ALB DNS: $ALB_DNS"
echo "Testing: https://$ALB_DNS/api/actuator/health"

# Test health endpoint
curl -v -m 10 "https://$ALB_DNS/api/actuator/health" 2>&1 | head -20

echo ""
echo "8. Getting Recent Application Logs..."
echo "------------------------------------"
LOG_GROUP="/ecs/healthapp"

# Get the most recent log stream
LATEST_STREAM=$(aws logs describe-log-streams \
  --log-group-name $LOG_GROUP \
  --order-by LastEventTime \
  --descending \
  --max-items 1 \
  --region $REGION \
  --query 'logStreams[0].logStreamName' \
  --output text 2>/dev/null)

if [ "$LATEST_STREAM" != "None" ] && [ "$LATEST_STREAM" != "" ]; then
  echo "Latest log stream: $LATEST_STREAM"
  echo "Recent logs:"
  aws logs get-log-events \
    --log-group-name $LOG_GROUP \
    --log-stream-name "$LATEST_STREAM" \
    --region $REGION \
    --limit 20 \
    --query 'events[].message' \
    --output text 2>/dev/null | tail -10
else
  echo "No log streams found or error accessing logs"
fi

echo ""
echo "✅ Debug script completed!"
echo "Check the output above for any issues." 