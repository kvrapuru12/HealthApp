#!/bin/bash

echo "🔍 Deep Network Analysis"
echo "======================="

REGION="us-east-1"
ECS_SG="sg-0e58b0d4b9688416c"
RDS_SG="sg-02323c2d3d6330ed8"
RDS_ENDPOINT="healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com"

echo ""
echo "1. Checking RDS Security Group Rules..."
echo "--------------------------------------"
echo "RDS Security Group: $RDS_SG"
aws ec2 describe-security-group-rules --filters Name=group-id,Values=$RDS_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp` && FromPort==`3306`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId,Source:ReferencedGroupId}' --output table

echo ""
echo "2. Checking ECS Security Group Rules..."
echo "--------------------------------------"
echo "ECS Security Group: $ECS_SG"
aws ec2 describe-security-group-rules --filters Name=group-id,Values=$ECS_SG --region $REGION --query 'SecurityGroupRules[?IpProtocol==`tcp`].{Type:IsEgress,Port:FromPort,Source:ReferencedGroupId}' --output table

echo ""
echo "3. Checking RDS Subnet Group Subnets..."
echo "--------------------------------------"
RDS_SUBNET_GROUP="healthapp-db-subnet"
aws rds describe-db-subnet-groups --db-subnet-group-name $RDS_SUBNET_GROUP --region $REGION --query 'DBSubnetGroups[0].Subnets[].{SubnetId:SubnetId,AvailabilityZone:SubnetAvailabilityZone}' --output table

echo ""
echo "4. Checking ECS Subnet..."
echo "------------------------"
ECS_SUBNET=$(aws ecs describe-services --cluster healthapp-cluster --services healthapp-service --region $REGION --query 'services[0].networkConfiguration.awsvpcConfiguration.subnets[0]' --output text 2>/dev/null)
echo "ECS Subnet: $ECS_SUBNET"

aws ec2 describe-subnets --subnet-ids $ECS_SUBNET --region $REGION --query 'Subnets[0].{VpcId:VpcId,AvailabilityZone:AvailabilityZone,RouteTableId:RouteTableId,CidrBlock:CidrBlock}' --output table

echo ""
echo "5. Checking Route Tables..."
echo "---------------------------"
ECS_ROUTE_TABLE=$(aws ec2 describe-subnets --subnet-ids $ECS_SUBNET --region $REGION --query 'Subnets[0].RouteTableId' --output text 2>/dev/null)
echo "ECS Route Table: $ECS_ROUTE_TABLE"

aws ec2 describe-route-tables --route-table-ids $ECS_ROUTE_TABLE --region $REGION --query 'RouteTables[0].Routes[].{Destination:DestinationCidrBlock,Target:GatewayId,TargetType:GatewayId}' --output table

echo ""
echo "6. Testing RDS Connectivity from Different Perspectives..."
echo "--------------------------------------------------------"

echo "Testing from local machine:"
nc -zv $RDS_ENDPOINT 3306 2>&1

echo ""
echo "7. Checking RDS Instance Details..."
echo "----------------------------------"
aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].{Status:DBInstanceStatus,Engine:Engine,EngineVersion:EngineVersion,PubliclyAccessible:PubliclyAccessible,MultiAZ:MultiAZ}' --output table

echo ""
echo "8. Checking if RDS is in Maintenance Mode..."
echo "--------------------------------------------"
aws rds describe-db-instances --db-instance-identifier healthapp-db --region $REGION --query 'DBInstances[0].{Status:DBInstanceStatus,StorageEncrypted:StorageEncrypted,DeletionProtection:DeletionProtection}' --output table

echo ""
echo "9. Checking ECS Task Network Interface..."
echo "----------------------------------------"
TASK_ARN=$(aws ecs list-tasks --cluster healthapp-cluster --region $REGION --query 'taskArns[0]' --output text 2>/dev/null)
echo "Task ARN: $TASK_ARN"

if [ "$TASK_ARN" != "None" ] && [ "$TASK_ARN" != "" ]; then
    aws ecs describe-tasks --cluster healthapp-cluster --tasks $TASK_ARN --region $REGION --query 'tasks[0].attachments[0].{Type:type,Status:status,Details:details[?name==`networkInterfaceId`].value}' --output table
fi

echo ""
echo "🔍 Analysis Summary:"
echo "1. VPC: Same ✅"
echo "2. Security Groups: Need to verify rules"
echo "3. Subnet Routing: Need to check"
echo "4. RDS Status: Need to verify"
echo "5. Network Interface: Need to check" 