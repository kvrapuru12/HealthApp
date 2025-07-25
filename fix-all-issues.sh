#!/bin/bash

echo "🔧 Fixing All HealthApp Issues"
echo "=============================="

REGION="us-east-1"
ECS_SG="sg-0e58b0d4b9688416c"
ALB_SG="sg-008d41230f5a6659c"
RDS_SG="sg-0e58b0d4b9688416c"  # Assuming RDS uses same security group

echo ""
echo "1. Checking RDS Security Group..."
echo "--------------------------------"
echo "RDS Security Group: $RDS_SG"
aws ec2 describe-security-group-rules --filters Name=group-id,Values=$RDS_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp` && FromPort==`3306`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output table

echo ""
echo "2. Checking ECS Security Group..."
echo "--------------------------------"
echo "ECS Security Group: $ECS_SG"
aws ec2 describe-security-group-rules --filters Name=group-id,Values=$ECS_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output table

echo ""
echo "3. Checking ALB Security Group..."
echo "--------------------------------"
echo "ALB Security Group: $ALB_SG"
aws ec2 describe-security-group-rules --filters Name=group-id,Values=$ALB_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output table

echo ""
echo "4. Fixing Security Group Rules..."
echo "--------------------------------"

# Add rule to allow ECS to RDS (port 3306)
echo "Adding rule: ECS -> RDS (port 3306)"
aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG \
  --protocol tcp \
  --port 3306 \
  --source-group $ECS_SG \
  --region $REGION 2>/dev/null || echo "Rule already exists or error"

# Add rule to allow ALB to ECS (port 8080)
echo "Adding rule: ALB -> ECS (port 8080)"
aws ec2 authorize-security-group-ingress \
  --group-id $ECS_SG \
  --protocol tcp \
  --port 8080 \
  --source-group $ALB_SG \
  --region $REGION 2>/dev/null || echo "Rule already exists or error"

# Add rule to allow ALB HTTP (port 80)
echo "Adding rule: ALB HTTP (port 80)"
aws ec2 authorize-security-group-ingress \
  --group-id $ALB_SG \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0 \
  --region $REGION 2>/dev/null || echo "Rule already exists or error"

# Add rule to allow ALB HTTPS (port 443)
echo "Adding rule: ALB HTTPS (port 443)"
aws ec2 authorize-security-group-ingress \
  --group-id $ALB_SG \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0 \
  --region $REGION 2>/dev/null || echo "Rule already exists or error"

echo ""
echo "5. Checking RDS Status..."
echo "------------------------"
aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].{Status:DBInstanceStatus,Endpoint:Endpoint.Address,Port:Endpoint.Port}' --output table

echo ""
echo "6. Testing Database Connectivity..."
echo "----------------------------------"
RDS_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].Endpoint.Address' --output text 2>/dev/null)
echo "RDS Endpoint: $RDS_ENDPOINT"

if [ "$RDS_ENDPOINT" != "None" ] && [ "$RDS_ENDPOINT" != "" ]; then
    echo "Testing connection to RDS..."
    nc -zv $RDS_ENDPOINT 3306 2>&1 || echo "Cannot connect to RDS - check security groups"
fi

echo ""
echo "7. Force New ECS Deployment..."
echo "------------------------------"
aws ecs update-service --cluster healthapp-cluster --service healthapp-service --force-new-deployment --region $REGION

echo ""
echo "8. Checking Application Logs..."
echo "------------------------------"
LOG_STREAM=$(aws logs describe-log-streams --log-group-name "/ecs/healthapp" --order-by LastEventTime --descending --max-items 1 --region $REGION --query 'logStreams[0].logStreamName' --output text 2>/dev/null)
echo "Latest log stream: $LOG_STREAM"

if [ "$LOG_STREAM" != "None" ] && [ "$LOG_STREAM" != "" ]; then
    echo "Recent logs:"
    aws logs get-log-events --log-group-name "/ecs/healthapp" --log-stream-name "$LOG_STREAM" --region $REGION --limit 10 --query 'events[].message' --output text 2>/dev/null | tail -5
fi

echo ""
echo "✅ Fix script completed!"
echo ""
echo "🔍 Next steps:"
echo "1. Wait 2-3 minutes for the new deployment"
echo "2. Check the application logs for startup issues"
echo "3. Test the health endpoint: http://healthapp-alb-1571435665.us-east-1.elb.amazonaws.com/api/actuator/health"
echo "4. Monitor the target group health in AWS console" 