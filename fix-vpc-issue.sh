#!/bin/bash

echo "🔧 Fixing VPC Mismatch Issue"
echo "============================="

REGION="us-east-1"
RDS_VPC="vpc-01739f4aaac8dc6ba"

echo ""
echo "1. Getting ECS VPC Information..."
echo "--------------------------------"
ECS_SUBNET=$(aws ecs describe-services --cluster healthapp-cluster --services healthapp-service --region $REGION --query 'services[0].networkConfiguration.awsvpcConfiguration.subnets[0]' --output text 2>/dev/null)
echo "ECS Subnet: $ECS_SUBNET"

# Get VPC from subnet
ECS_VPC=$(aws ec2 describe-subnets --subnet-ids $ECS_SUBNET --region $REGION --query 'Subnets[0].VpcId' --output text 2>/dev/null)
echo "ECS VPC: $ECS_VPC"

echo ""
echo "2. VPC Comparison..."
echo "-------------------"
echo "RDS VPC: $RDS_VPC"
echo "ECS VPC: $ECS_VPC"

if [ "$RDS_VPC" = "$ECS_VPC" ]; then
    echo "✅ RDS and ECS are in the same VPC"
    echo "The issue might be elsewhere"
else
    echo "❌ RDS and ECS are in different VPCs"
    echo "This is the root cause of the connectivity issue!"
fi

echo ""
echo "3. Checking RDS Subnet Group..."
echo "-------------------------------"
RDS_SUBNET_GROUP="healthapp-db-subnet"
aws rds describe-db-subnet-groups --db-subnet-group-name $RDS_SUBNET_GROUP --region $REGION --query 'DBSubnetGroups[0].{VpcId:VpcId,Subnets:Subnets[].SubnetId}' --output table

echo ""
echo "4. Checking ECS Subnet Details..."
echo "--------------------------------"
aws ec2 describe-subnets --subnet-ids $ECS_SUBNET --region $REGION --query 'Subnets[0].{VpcId:VpcId,AvailabilityZone:AvailabilityZone,RouteTableId:RouteTableId}' --output table

echo ""
echo "5. Solution Options..."
echo "---------------------"
echo "Option 1: Move RDS to ECS VPC (Recommended)"
echo "  - Create new subnet group in ECS VPC"
echo "  - Modify RDS to use new subnet group"
echo ""
echo "Option 2: Move ECS to RDS VPC"
echo "  - Update ECS service to use subnets in RDS VPC"
echo ""
echo "Option 3: VPC Peering"
echo "  - Create VPC peering between the two VPCs"
echo "  - Update route tables"
echo ""
echo "6. Recommended Action..."
echo "-----------------------"
echo "The easiest solution is to move RDS to the ECS VPC."
echo "This requires:"
echo "1. Creating a new subnet group in the ECS VPC"
echo "2. Modifying the RDS instance to use the new subnet group"
echo "3. Ensuring the subnets have proper routing"

echo ""
echo "🔍 Current Status:"
echo "- RDS is publicly accessible: ✅"
echo "- Security group rules exist: ✅"
echo "- VPC mismatch: ❌ (This is the problem)"
echo "- Application can't connect to database: ❌"
echo "- Load balancer gets 502: ❌" 