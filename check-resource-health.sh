#!/bin/bash

echo "🏥 HealthApp Resource Health Check"
echo "=================================="

REGION="us-east-1"
CLUSTER="healthapp-cluster"
SERVICE="healthapp-service"
ALB="healthapp-alb"
TARGET_GROUP="healthapp-tg"
RDS_INSTANCE="healthapp-db"

echo ""
echo "1. ECS Cluster Status..."
echo "-----------------------"
echo "Cluster: $CLUSTER"
aws ecs describe-clusters --clusters $CLUSTER --region $REGION --query 'clusters[0].{status:status,activeServicesCount:activeServicesCount,runningTasksCount:runningTasksCount}' --output table 2>/dev/null || echo "Error getting cluster status"

echo ""
echo "2. ECS Service Status..."
echo "-----------------------"
echo "Service: $SERVICE"
aws ecs describe-services --cluster $CLUSTER --services $SERVICE --region $REGION --query 'services[0].{status:status,runningCount:runningCount,desiredCount:desiredCount,pendingCount:pendingCount}' --output table 2>/dev/null || echo "Error getting service status"

echo ""
echo "3. ECS Tasks Status..."
echo "---------------------"
echo "Running tasks:"
TASKS=$(aws ecs list-tasks --cluster $CLUSTER --region $REGION --query 'taskArns' --output text 2>/dev/null)
if [ "$TASKS" != "None" ] && [ "$TASKS" != "" ]; then
    echo "Found tasks: $TASKS"
    aws ecs describe-tasks --cluster $CLUSTER --tasks $TASKS --region $REGION --query 'tasks[].{taskArn:taskArn,lastStatus:lastStatus,desiredStatus:desiredStatus,healthStatus:healthStatus}' --output table 2>/dev/null || echo "Error getting task details"
else
    echo "No running tasks found"
fi

echo ""
echo "4. Load Balancer Status..."
echo "-------------------------"
echo "ALB: $ALB"
aws elbv2 describe-load-balancers --names $ALB --region $REGION --query 'LoadBalancers[0].{DNSName:DNSName,State:State.Code,Scheme:Scheme}' --output table 2>/dev/null || echo "Error getting ALB status"

echo ""
echo "5. Target Group Health..."
echo "------------------------"
TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups --names $TARGET_GROUP --region $REGION --query 'TargetGroups[0].TargetGroupArn' --output text 2>/dev/null)
echo "Target Group ARN: $TARGET_GROUP_ARN"

if [ "$TARGET_GROUP_ARN" != "None" ] && [ "$TARGET_GROUP_ARN" != "" ]; then
    echo "Target Health:"
    aws elbv2 describe-target-health --target-group-arn $TARGET_GROUP_ARN --region $REGION --output table 2>/dev/null || echo "Error getting target health"
else
    echo "Target group not found"
fi

echo ""
echo "6. RDS Database Status..."
echo "------------------------"
aws rds describe-db-instances --db-instance-identifier $RDS_INSTANCE --region $REGION --query 'DBInstances[0].{DBInstanceStatus:DBInstanceStatus,DBInstanceClass:DBInstanceClass,Engine:Engine,Endpoint:Endpoint}' --output table 2>/dev/null || echo "Error getting RDS status"

echo ""
echo "7. Security Groups..."
echo "-------------------"
# Get ALB security group
ALB_SG=$(aws elbv2 describe-load-balancers --names $ALB --region $REGION --query 'LoadBalancers[0].SecurityGroups[0]' --output text 2>/dev/null)
echo "ALB Security Group: $ALB_SG"

if [ "$ALB_SG" != "None" ] && [ "$ALB_SG" != "" ]; then
    echo "ALB Security Group Rules:"
    aws ec2 describe-security-group-rules --filters Name=group-id,Values=$ALB_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output table 2>/dev/null || echo "Error getting ALB security group rules"
fi

# Get ECS service security group
ECS_SG=$(aws ecs describe-services --cluster $CLUSTER --services $SERVICE --region $REGION --query 'services[0].networkConfiguration.awsvpcConfiguration.securityGroups[0]' --output text 2>/dev/null)
echo "ECS Security Group: $ECS_SG"

if [ "$ECS_SG" != "None" ] && [ "$ECS_SG" != "" ]; then
    echo "ECS Security Group Rules:"
    aws ec2 describe-security-group-rules --filters Name=group-id,Values=$ECS_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output table 2>/dev/null || echo "Error getting ECS security group rules"
fi

echo ""
echo "8. Recent Application Logs..."
echo "----------------------------"
LOG_GROUP="/ecs/healthapp"
LATEST_STREAM=$(aws logs describe-log-streams --log-group-name $LOG_GROUP --order-by LastEventTime --descending --max-items 1 --region $REGION --query 'logStreams[0].logStreamName' --output text 2>/dev/null)

if [ "$LATEST_STREAM" != "None" ] && [ "$LATEST_STREAM" != "" ]; then
    echo "Latest log stream: $LATEST_STREAM"
    echo "Recent logs (last 10 lines):"
    aws logs get-log-events --log-group-name $LOG_GROUP --log-stream-name "$LATEST_STREAM" --region $REGION --limit 10 --query 'events[].message' --output text 2>/dev/null | tail -10 || echo "Error getting logs"
else
    echo "No log streams found"
fi

echo ""
echo "9. Testing Connectivity..."
echo "-------------------------"
ALB_DNS=$(aws elbv2 describe-load-balancers --names $ALB --region $REGION --query 'LoadBalancers[0].DNSName' --output text 2>/dev/null)
echo "ALB DNS: $ALB_DNS"

if [ "$ALB_DNS" != "None" ] && [ "$ALB_DNS" != "" ]; then
    echo "Testing HTTP health endpoint:"
    curl -v -m 10 "http://$ALB_DNS/api/actuator/health" 2>&1 | head -15 || echo "HTTP test failed"
    
    echo ""
    echo "Testing HTTPS health endpoint:"
    curl -v -m 10 "https://$ALB_DNS/api/actuator/health" 2>&1 | head -15 || echo "HTTPS test failed"
fi

echo ""
echo "10. ECS Service Events..."
echo "------------------------"
aws ecs describe-services --cluster $CLUSTER --services $SERVICE --region $REGION --query 'services[0].events[0:3]' --output table 2>/dev/null || echo "Error getting service events"

echo ""
echo "✅ Resource health check completed!"
echo ""
echo "🔍 Key things to check:"
echo "1. Are ECS tasks running?"
echo "2. Are targets healthy in the target group?"
echo "3. Can the application connect to the database?"
echo "4. Are security groups allowing proper traffic?"
echo "5. Is the application starting successfully?" 