#!/bin/bash

echo "🎯 Focused Debug - HealthApp Issues"
echo "==================================="

REGION="us-east-1"
CLUSTER="healthapp-cluster"
SERVICE="healthapp-service"
LOG_STREAM="ecs/healthapp/12cb0d1404de4e658f0c1ddf9bb1babe"

echo ""
echo "1. Checking ECS Tasks Status..."
echo "-------------------------------"
echo "Running tasks:"
aws ecs list-tasks --cluster $CLUSTER --region $REGION --output text 2>/dev/null || echo "Error getting tasks"

echo ""
echo "2. Checking Task Definition..."
echo "-----------------------------"
aws ecs describe-task-definition --task-definition healthapp-task --region $REGION --query 'taskDefinition.{family:family,revision:revision,networkMode:networkMode}' --output table 2>/dev/null || echo "Error getting task definition"

echo ""
echo "3. Checking Security Groups..."
echo "-----------------------------"
# Get the security group from the load balancer
ALB_SG=$(aws elbv2 describe-load-balancers --names healthapp-alb --region $REGION --query 'LoadBalancers[0].SecurityGroups[0]' --output text 2>/dev/null)
echo "ALB Security Group: $ALB_SG"

if [ "$ALB_SG" != "None" ] && [ "$ALB_SG" != "" ]; then
  echo "ALB Security Group Rules:"
  aws ec2 describe-security-group-rules --filters Name=group-id,Values=$ALB_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output table 2>/dev/null || echo "Error getting security group rules"
fi

echo ""
echo "4. Checking Target Group Configuration..."
echo "----------------------------------------"
TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups --names healthapp-tg --region $REGION --query 'TargetGroups[0].TargetGroupArn' --output text 2>/dev/null)
echo "Target Group ARN: $TARGET_GROUP_ARN"

if [ "$TARGET_GROUP_ARN" != "None" ] && [ "$TARGET_GROUP_ARN" != "" ]; then
  echo "Target Group Health:"
  aws elbv2 describe-target-health --target-group-arn $TARGET_GROUP_ARN --region $REGION --output table 2>/dev/null || echo "Error getting target health"
fi

echo ""
echo "5. Checking Recent Application Logs..."
echo "-------------------------------------"
echo "Getting logs from stream: $LOG_STREAM"
aws logs get-log-events \
  --log-group-name "/ecs/healthapp" \
  --log-stream-name "$LOG_STREAM" \
  --region $REGION \
  --limit 30 \
  --query 'events[].message' \
  --output text 2>/dev/null | tail -20 || echo "Error getting logs"

echo ""
echo "6. Testing HTTP (port 80) instead of HTTPS..."
echo "---------------------------------------------"
ALB_DNS="healthapp-alb-1571435665.us-east-1.elb.amazonaws.com"
echo "Testing: http://$ALB_DNS/api/actuator/health"
curl -v -m 10 "http://$ALB_DNS/api/actuator/health" 2>&1 | head -15

echo ""
echo "7. Checking Load Balancer Listeners..."
echo "-------------------------------------"
aws elbv2 describe-listeners \
  --load-balancer-arn $(aws elbv2 describe-load-balancers --names healthapp-alb --region $REGION --query 'LoadBalancers[0].LoadBalancerArn' --output text 2>/dev/null) \
  --region $REGION \
  --query 'Listeners[].{Port:Port,Protocol:Protocol,DefaultActions:DefaultActions[0].Type}' \
  --output table 2>/dev/null || echo "Error getting listeners"

echo ""
echo "✅ Focused debug completed!" 