#!/bin/bash

echo "✅ Final HealthApp Verification"
echo "==============================="

REGION="us-east-1"
ALB_DNS="healthapp-alb-1571435665.us-east-1.elb.amazonaws.com"

echo ""
echo "1. Checking ECS Service Status..."
echo "--------------------------------"
aws ecs describe-services --cluster healthapp-cluster --services healthapp-service --region $REGION --query 'services[0].{status:status,runningCount:runningCount,desiredCount:desiredCount,pendingCount:pendingCount}' --output table

echo ""
echo "2. Checking Target Group Health..."
echo "--------------------------------"
TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups --names healthapp-tg --region $REGION --query 'TargetGroups[0].TargetGroupArn' --output text 2>/dev/null)
aws elbv2 describe-target-health --target-group-arn $TARGET_GROUP_ARN --region $REGION --output table

echo ""
echo "3. Testing Health Endpoint..."
echo "----------------------------"
echo "Testing: http://$ALB_DNS/api/actuator/health"
curl -v -m 10 "http://$ALB_DNS/api/actuator/health" 2>&1 | head -20

echo ""
echo "4. Checking Recent Application Logs..."
echo "------------------------------------"
LOG_STREAM=$(aws logs describe-log-streams --log-group-name "/ecs/healthapp" --order-by LastEventTime --descending --max-items 1 --region $REGION --query 'logStreams[0].logStreamName' --output text 2>/dev/null)

if [ "$LOG_STREAM" != "None" ] && [ "$LOG_STREAM" != "" ]; then
    echo "Latest log stream: $LOG_STREAM"
    echo "Recent logs (last 10 lines):"
    aws logs get-log-events --log-group-name "/ecs/healthapp" --log-stream-name "$LOG_STREAM" --region $REGION --limit 10 --query 'events[].message' --output text 2>/dev/null | tail -10
else
    echo "No log streams found"
fi

echo ""
echo "5. Checking Subnet Route Table Association..."
echo "--------------------------------------------"
ECS_SUBNET=$(aws ecs describe-services --cluster healthapp-cluster --services healthapp-service --region $REGION --query 'services[0].networkConfiguration.awsvpcConfiguration.subnets[0]' --output text 2>/dev/null)
aws ec2 describe-subnets --subnet-ids $ECS_SUBNET --region $REGION --query 'Subnets[0].{SubnetId:SubnetId,RouteTableId:RouteTableId,AvailabilityZone:AvailabilityZone}' --output table

echo ""
echo "6. Testing Application Endpoints..."
echo "---------------------------------"
echo "Testing API endpoints:"
echo "1. Health endpoint:"
curl -s -o /dev/null -w "%{http_code}" "http://$ALB_DNS/api/actuator/health" 2>/dev/null || echo "Failed"

echo ""
echo "2. Users endpoint:"
curl -s -o /dev/null -w "%{http_code}" "http://$ALB_DNS/api/users" 2>/dev/null || echo "Failed"

echo ""
echo "7. Summary..."
echo "------------"
echo "🔍 Current Status:"
echo "- ECS Service: Check above"
echo "- Target Health: Check above"
echo "- Health Endpoint: Check above"
echo "- Application Logs: Check above"
echo "- Subnet Routing: Check above"
echo ""
echo "🎯 Expected Results:"
echo "- ECS runningCount should be > 0"
echo "- Target health should show 'healthy'"
echo "- Health endpoint should return 200 OK"
echo "- Application logs should show successful startup"
echo "- Subnet should have a RouteTableId"
echo ""
echo "✅ If all above are successful, your HealthApp is working!"
echo ""
echo "🌐 Application URLs:"
echo "   API Base: http://$ALB_DNS/api"
echo "   Health Check: http://$ALB_DNS/api/actuator/health"
echo "   Swagger UI: http://$ALB_DNS/api/swagger-ui.html" 