#!/bin/bash

echo "🌐 Checking Network Configuration"
echo "================================"

REGION="us-east-1"
ECS_SG="sg-0e58b0d4b9688416c"
RDS_SG="sg-02323c2d3d6330ed8"

echo ""
echo "1. RDS Network Configuration..."
echo "-------------------------------"
aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].{DBSubnetGroup:DBSubnetGroup.DBSubnetGroupName,VpcId:DBSubnetGroup.VpcId,PubliclyAccessible:PubliclyAccessible}' --output table

echo ""
echo "2. RDS Subnet Group Details..."
echo "------------------------------"
SUBNET_GROUP=$(aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].DBSubnetGroup.DBSubnetGroupName' --output text 2>/dev/null)
echo "Subnet Group: $SUBNET_GROUP"

if [ "$SUBNET_GROUP" != "None" ] && [ "$SUBNET_GROUP" != "" ]; then
    aws rds describe-db-subnet-groups --db-subnet-group-name $SUBNET_GROUP --region $REGION --query 'DBSubnetGroups[0].Subnets[].{SubnetId:SubnetId,AvailabilityZone:SubnetAvailabilityZone}' --output table
fi

echo ""
echo "3. ECS Service Network Configuration..."
echo "--------------------------------------"
aws ecs describe-services --cluster healthapp-cluster --services healthapp-service --region $REGION --query 'services[0].networkConfiguration.awsvpcConfiguration.{subnets:subnets,securityGroups:securityGroups,assignPublicIp:assignPublicIp}' --output table

echo ""
echo "4. Checking VPC Configuration..."
echo "-------------------------------"
RDS_VPC=$(aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].DBSubnetGroup.VpcId' --output text 2>/dev/null)
ECS_VPC=$(aws ecs describe-services --cluster healthapp-cluster --services healthapp-service --region $REGION --query 'services[0].networkConfiguration.awsvpcConfiguration.subnets[0]' --output text 2>/dev/null | cut -d'/' -f1)

echo "RDS VPC: $RDS_VPC"
echo "ECS VPC: $ECS_VPC"

if [ "$RDS_VPC" = "$ECS_VPC" ]; then
    echo "✅ RDS and ECS are in the same VPC"
else
    echo "❌ RDS and ECS are in different VPCs - this is the problem!"
fi

echo ""
echo "5. Checking Route Tables..."
echo "---------------------------"
if [ "$RDS_VPC" != "None" ] && [ "$RDS_VPC" != "" ]; then
    echo "RDS VPC Route Tables:"
    aws ec2 describe-route-tables --filters Name=vpc-id,Values=$RDS_VPC --region $REGION --query 'RouteTables[].{RouteTableId:RouteTableId,Routes:Routes[?GatewayId!=`null`].GatewayId}' --output table
fi

echo ""
echo "6. Checking if RDS is Publicly Accessible..."
echo "--------------------------------------------"
PUBLIC_ACCESS=$(aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].PubliclyAccessible' --output text 2>/dev/null)
echo "RDS Publicly Accessible: $PUBLIC_ACCESS"

if [ "$PUBLIC_ACCESS" = "True" ]; then
    echo "✅ RDS is publicly accessible"
else
    echo "❌ RDS is not publicly accessible - this might be the issue"
fi

echo ""
echo "7. Testing from ECS Task Perspective..."
echo "--------------------------------------"
echo "Since we can't directly test from ECS, let's check if the issue is:"
echo "1. VPC mismatch"
echo "2. Subnet routing"
echo "3. Security group configuration"
echo "4. RDS accessibility"

echo ""
echo "🔍 Summary of potential issues:"
echo "1. RDS and ECS might be in different VPCs"
echo "2. RDS might not be publicly accessible"
echo "3. Subnet routing might be incorrect"
echo "4. Security group rules might be misconfigured" 