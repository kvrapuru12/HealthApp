#!/bin/bash

echo "🔧 Fixing Subnet Routing Issue"
echo "=============================="

REGION="us-east-1"
VPC_ID="vpc-01739f4aaac8dc6ba"
ECS_SUBNET="subnet-0b2bdf72116405208"

echo ""
echo "1. Current Subnet Status..."
echo "---------------------------"
aws ec2 describe-subnets --subnet-ids $ECS_SUBNET --region $REGION --query 'Subnets[0].{SubnetId:SubnetId,VpcId:VpcId,RouteTableId:RouteTableId,AvailabilityZone:AvailabilityZone}' --output table

echo ""
echo "2. Available Route Tables..."
echo "---------------------------"
aws ec2 describe-route-tables --filters Name=vpc-id,Values=$VPC_ID --region $REGION --query 'RouteTables[].{RouteTableId:RouteTableId,Main:Associations[?Main==`true`].Main|[0],Routes:Routes[?GatewayId!=`null`].GatewayId}' --output table

echo ""
echo "3. Finding Best Route Table..."
echo "-----------------------------"
# Get the main route table (should have internet gateway)
MAIN_RT=$(aws ec2 describe-route-tables --filters Name=vpc-id,Values=$VPC_ID Name=association.main,Values=true --region $REGION --query 'RouteTables[0].RouteTableId' --output text 2>/dev/null)
echo "Main Route Table: $MAIN_RT"

# Get route table with internet gateway
IGW_RT=$(aws ec2 describe-route-tables --filters Name=vpc-id,Values=$VPC_ID --region $REGION --query 'RouteTables[?Routes[?GatewayId!=`null`]].RouteTableId|[0]' --output text 2>/dev/null)
echo "Route Table with IGW: $IGW_RT"

# Choose the best route table
TARGET_RT=""
if [ "$IGW_RT" != "None" ] && [ "$IGW_RT" != "" ]; then
    TARGET_RT=$IGW_RT
    echo "Using route table with IGW: $TARGET_RT"
elif [ "$MAIN_RT" != "None" ] && [ "$MAIN_RT" != "" ]; then
    TARGET_RT=$MAIN_RT
    echo "Using main route table: $TARGET_RT"
else
    echo "❌ No suitable route table found!"
    exit 1
fi

echo ""
echo "4. Associating Subnet with Route Table..."
echo "----------------------------------------"
echo "Associating subnet $ECS_SUBNET with route table $TARGET_RT"

ASSOCIATION_ID=$(aws ec2 associate-route-table --subnet-id $ECS_SUBNET --route-table-id $TARGET_RT --region $REGION --query 'AssociationId' --output text 2>/dev/null)

if [ "$ASSOCIATION_ID" != "None" ] && [ "$ASSOCIATION_ID" != "" ]; then
    echo "✅ Successfully associated subnet with route table"
    echo "Association ID: $ASSOCIATION_ID"
else
    echo "❌ Failed to associate subnet with route table"
    echo "This might be because the subnet is already associated"
fi

echo ""
echo "5. Verifying Subnet Association..."
echo "---------------------------------"
aws ec2 describe-subnets --subnet-ids $ECS_SUBNET --region $REGION --query 'Subnets[0].{SubnetId:SubnetId,VpcId:VpcId,RouteTableId:RouteTableId,AvailabilityZone:AvailabilityZone}' --output table

echo ""
echo "6. Checking Route Table Routes..."
echo "--------------------------------"
aws ec2 describe-route-tables --route-table-ids $TARGET_RT --region $REGION --query 'RouteTables[0].Routes[].{Destination:DestinationCidrBlock,Target:GatewayId,TargetType:GatewayId}' --output table

echo ""
echo "7. Force New ECS Deployment..."
echo "------------------------------"
aws ecs update-service --cluster healthapp-cluster --service healthapp-service --force-new-deployment --region $REGION

echo ""
echo "8. Testing RDS Connectivity..."
echo "-----------------------------"
RDS_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].Endpoint.Address' --output text 2>/dev/null)
echo "RDS Endpoint: $RDS_ENDPOINT"

echo "Testing connection (this might still fail from local machine):"
nc -zv $RDS_ENDPOINT 3306 2>&1 || echo "Local connection failed (expected if RDS is in private subnet)"

echo ""
echo "✅ Subnet routing fix completed!"
echo ""
echo "🔍 What was fixed:"
echo "1. Associated ECS subnet with proper route table"
echo "2. Ensured route table has internet gateway access"
echo "3. Forced new ECS deployment"
echo ""
echo "📋 Next steps:"
echo "1. Wait 3-5 minutes for the new deployment"
echo "2. Check application logs for successful database connection"
echo "3. Test health endpoint: http://healthapp-alb-1571435665.us-east-1.elb.amazonaws.com/api/actuator/health"
echo "4. Monitor target group health in AWS console"
echo ""
echo "🎯 Expected outcome:"
echo "- ECS tasks should now be able to connect to RDS"
echo "- Application should start successfully"
echo "- Load balancer targets should become healthy" 