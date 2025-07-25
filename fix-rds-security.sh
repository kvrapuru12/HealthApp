#!/bin/bash

echo "🔧 Fixing RDS Security Group Configuration"
echo "=========================================="

REGION="us-east-1"
ECS_SG="sg-0e58b0d4b9688416c"

echo ""
echo "1. Getting RDS Security Group..."
echo "--------------------------------"
RDS_SG=$(aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' --output text 2>/dev/null)
echo "RDS Security Group: $RDS_SG"

if [ "$RDS_SG" = "None" ] || [ "$RDS_SG" = "" ]; then
    echo "❌ Could not get RDS security group"
    exit 1
fi

echo ""
echo "2. Checking Current RDS Security Group Rules..."
echo "-----------------------------------------------"
aws ec2 describe-security-group-rules --filters Name=group-id,Values=$RDS_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp` && FromPort==`3306`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId,Source:ReferencedGroupId}' --output table

echo ""
echo "3. Adding ECS to RDS Access Rule..."
echo "-----------------------------------"
echo "Adding rule: ECS Security Group ($ECS_SG) -> RDS (port 3306)"
aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG \
  --protocol tcp \
  --port 3306 \
  --source-group $ECS_SG \
  --region $REGION

if [ $? -eq 0 ]; then
    echo "✅ Successfully added ECS to RDS access rule"
else
    echo "❌ Failed to add rule or rule already exists"
fi

echo ""
echo "4. Verifying the Rule..."
echo "------------------------"
aws ec2 describe-security-group-rules --filters Name=group-id,Values=$RDS_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp` && FromPort==`3306`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output table

echo ""
echo "5. Testing Database Connectivity..."
echo "----------------------------------"
RDS_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].Endpoint.Address' --output text 2>/dev/null)
echo "RDS Endpoint: $RDS_ENDPOINT"

if [ "$RDS_ENDPOINT" != "None" ] && [ "$RDS_ENDPOINT" != "" ]; then
    echo "Testing connection to RDS..."
    nc -zv $RDS_ENDPOINT 3306 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ Successfully connected to RDS!"
    else
        echo "❌ Still cannot connect to RDS"
    fi
fi

echo ""
echo "6. Force New ECS Deployment..."
echo "------------------------------"
aws ecs update-service --cluster healthapp-cluster --service healthapp-service --force-new-deployment --region $REGION

echo ""
echo "✅ RDS security group fix completed!"
echo ""
echo "🔍 Next steps:"
echo "1. Wait 2-3 minutes for the new deployment"
echo "2. Check if the application can now connect to the database"
echo "3. Monitor the application logs for successful startup"
echo "4. Test the health endpoint" 