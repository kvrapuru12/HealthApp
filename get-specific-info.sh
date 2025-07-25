#!/bin/bash

echo "🔍 Getting Specific Resource Information"
echo "========================================"

REGION="us-east-1"
TASK_ARN="arn:aws:ecs:us-east-1:114749311002:task/healthapp-cluster/55adfc9bd0b44ac1ba15af33cc815c61"
LOG_STREAM="ecs/healthapp/bac89b28e2ce4fec99c43f870611180a"

echo ""
echo "1. Task Status Details..."
echo "------------------------"
aws ecs describe-tasks --cluster healthapp-cluster --tasks $TASK_ARN --region $REGION --query 'tasks[0].{lastStatus:lastStatus,desiredStatus:desiredStatus,healthStatus:healthStatus,stoppedReason:stoppedReason}' --output json

echo ""
echo "2. Target Group Health Details..."
echo "--------------------------------"
aws elbv2 describe-target-health --target-group-arn arn:aws:elasticloadbalancing:us-east-1:114749311002:targetgroup/healthapp-tg/2bdf3906f5dceec6 --region $REGION --output json

echo ""
echo "3. Recent Application Logs..."
echo "----------------------------"
aws logs get-log-events --log-group-name "/ecs/healthapp" --log-stream-name "$LOG_STREAM" --region $REGION --limit 20 --query 'events[].message' --output text

echo ""
echo "4. RDS Database Status..."
echo "------------------------"
aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].{DBInstanceStatus:DBInstanceStatus,Endpoint:Endpoint,Port:Endpoint.Port}' --output json

echo ""
echo "5. Security Group Rules..."
echo "-------------------------"
# Get ECS security group
ECS_SG=$(aws ecs describe-services --cluster healthapp-cluster --services healthapp-service --region $REGION --query 'services[0].networkConfiguration.awsvpcConfiguration.securityGroups[0]' --output text 2>/dev/null)
echo "ECS Security Group: $ECS_SG"
aws ec2 describe-security-group-rules --filters Name=group-id,Values=$ECS_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output json 