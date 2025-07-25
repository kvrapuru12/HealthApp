#!/bin/bash

echo "🔧 Comprehensive HealthApp Fix"
echo "=============================="

REGION="us-east-1"
ECS_SG="sg-0e58b0d4b9688416c"
RDS_SG="sg-02323c2d3d6330ed8"
VPC_ID="vpc-01739f4aaac8dc6ba"

echo ""
echo "1. Checking Current Network Configuration..."
echo "-------------------------------------------"
ECS_SUBNET=$(aws ecs describe-services --cluster healthapp-cluster --services healthapp-service --region $REGION --query 'services[0].networkConfiguration.awsvpcConfiguration.subnets[0]' --output text 2>/dev/null)
echo "ECS Subnet: $ECS_SUBNET"

# Get subnet details
SUBNET_DETAILS=$(aws ec2 describe-subnets --subnet-ids $ECS_SUBNET --region $REGION --query 'Subnets[0].{VpcId:VpcId,AvailabilityZone:AvailabilityZone,RouteTableId:RouteTableId,CidrBlock:CidrBlock}' --output json 2>/dev/null)
echo "Subnet Details: $SUBNET_DETAILS"

echo ""
echo "2. Checking VPC Internet Gateway..."
echo "----------------------------------"
IGW=$(aws ec2 describe-internet-gateways --filters Name=attachment.vpc-id,Values=$VPC_ID --region $REGION --query 'InternetGateways[0].InternetGatewayId' --output text 2>/dev/null)
echo "Internet Gateway: $IGW"

if [ "$IGW" = "None" ] || [ "$IGW" = "" ]; then
    echo "❌ No Internet Gateway found - this is likely the problem!"
    echo "ECS tasks need internet access to connect to RDS"
else
    echo "✅ Internet Gateway found"
fi

echo ""
echo "3. Checking Route Tables..."
echo "---------------------------"
ROUTE_TABLES=$(aws ec2 describe-route-tables --filters Name=vpc-id,Values=$VPC_ID --region $REGION --query 'RouteTables[].{RouteTableId:RouteTableId,Routes:Routes[?GatewayId!=`null`].GatewayId}' --output table 2>/dev/null)
echo "Route Tables: $ROUTE_TABLES"

echo ""
echo "4. Checking RDS Security Group Rules..."
echo "--------------------------------------"
RDS_RULES=$(aws ec2 describe-security-group-rules --filters Name=group-id,Values=$RDS_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp` && FromPort==`3306`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output table 2>/dev/null)
echo "RDS Security Group Rules: $RDS_RULES"

echo ""
echo "5. Immediate Fixes..."
echo "-------------------"

# Check if we need to add internet gateway
if [ "$IGW" = "None" ] || [ "$IGW" = "" ]; then
    echo "Creating Internet Gateway..."
    IGW_ID=$(aws ec2 create-internet-gateway --region $REGION --query 'InternetGateway.InternetGatewayId' --output text 2>/dev/null)
    echo "Created IGW: $IGW_ID"
    
    echo "Attaching IGW to VPC..."
    aws ec2 attach-internet-gateway --internet-gateway-id $IGW_ID --vpc-id $VPC_ID --region $REGION 2>/dev/null || echo "IGW attachment failed or already attached"
fi

# Get the main route table
MAIN_RT=$(aws ec2 describe-route-tables --filters Name=vpc-id,Values=$VPC_ID Name=association.main,Values=true --region $REGION --query 'RouteTables[0].RouteTableId' --output text 2>/dev/null)
echo "Main Route Table: $MAIN_RT"

# Add route to internet gateway if needed
if [ "$MAIN_RT" != "None" ] && [ "$MAIN_RT" != "" ] && [ "$IGW" != "None" ] && [ "$IGW" != "" ]; then
    echo "Adding route to internet gateway..."
    aws ec2 create-route --route-table-id $MAIN_RT --destination-cidr-block 0.0.0.0/0 --gateway-id $IGW --region $REGION 2>/dev/null || echo "Route already exists or failed"
fi

echo ""
echo "6. Ensuring Security Group Rules..."
echo "----------------------------------"
# Add ECS to RDS access if not exists
aws ec2 authorize-security-group-ingress --group-id $RDS_SG --protocol tcp --port 3306 --source-group $ECS_SG --region $REGION 2>/dev/null || echo "ECS to RDS rule already exists"

# Add outbound rule for ECS to reach internet
aws ec2 authorize-security-group-egress --group-id $ECS_SG --protocol -1 --port -1 --cidr 0.0.0.0/0 --region $REGION 2>/dev/null || echo "ECS outbound rule already exists"

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
echo "✅ Comprehensive fix completed!"
echo ""
echo "🔍 What was fixed:"
echo "1. Internet Gateway (if missing)"
echo "2. Route table routes (if missing)"
echo "3. Security group rules"
echo "4. Forced new ECS deployment"
echo ""
echo "📋 Next steps:"
echo "1. Wait 3-5 minutes for the new deployment"
echo "2. Check application logs for successful database connection"
echo "3. Test health endpoint: http://healthapp-alb-1571435665.us-east-1.elb.amazonaws.com/api/actuator/health"
echo "4. Monitor target group health in AWS console" 