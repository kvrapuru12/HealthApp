#!/bin/bash

# HealthApp AWS Deployment Script
# This script automates the deployment process to AWS

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
AWS_REGION=${AWS_REGION:-"us-east-1"}
ECR_REPOSITORY="healthapp"
ECS_CLUSTER="healthapp-cluster"
ECS_SERVICE="healthapp-service"
ECS_TASK_DEFINITION="healthapp-task"

echo -e "${GREEN}🚀 HealthApp AWS Deployment Script${NC}"
echo "=================================="

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo -e "${RED}❌ AWS CLI is not installed. Please install it first.${NC}"
    exit 1
fi

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install it first.${NC}"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed. Please install it first.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ All prerequisites are met${NC}"

# Build the application
echo -e "${YELLOW}Building application...${NC}"
mvn clean package -DskipTests
echo -e "${GREEN}✅ Application built successfully${NC}"

# Build Docker image
echo -e "${YELLOW}Building Docker image...${NC}"
docker build -t healthapp .
echo -e "${GREEN}✅ Docker image built successfully${NC}"

# Get ECR login token
echo -e "${YELLOW}Logging into ECR...${NC}"
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $(aws sts get-caller-identity --query Account --output text).dkr.ecr.$AWS_REGION.amazonaws.com
echo -e "${GREEN}✅ Logged into ECR successfully${NC}"

# Get ECR repository URI
ECR_URI=$(aws ecr describe-repositories --repository-names $ECR_REPOSITORY --region $AWS_REGION --query 'repositories[0].repositoryUri' --output text)

if [ "$ECR_URI" = "None" ]; then
    echo -e "${YELLOW}Creating ECR repository...${NC}"
    aws ecr create-repository --repository-name $ECR_REPOSITORY --region $AWS_REGION
    ECR_URI=$(aws ecr describe-repositories --repository-names $ECR_REPOSITORY --region $AWS_REGION --query 'repositories[0].repositoryUri' --output text)
    echo -e "${GREEN}✅ ECR repository created: $ECR_URI${NC}"
fi

# Tag and push image
echo -e "${YELLOW}Tagging and pushing image to ECR...${NC}"
docker tag healthapp:latest $ECR_URI:latest
docker tag healthapp:latest $ECR_URI:$(git rev-parse --short HEAD)
docker push $ECR_URI:latest
docker push $ECR_URI:$(git rev-parse --short HEAD)
echo -e "${GREEN}✅ Image pushed to ECR successfully${NC}"

# Update ECS service
echo -e "${YELLOW}Updating ECS service...${NC}"
aws ecs update-service \
    --cluster $ECS_CLUSTER \
    --service $ECS_SERVICE \
    --force-new-deployment \
    --region $AWS_REGION

echo -e "${GREEN}✅ ECS service updated successfully${NC}"

# Wait for service to stabilize
echo -e "${YELLOW}Waiting for service to stabilize...${NC}"
aws ecs wait services-stable \
    --cluster $ECS_CLUSTER \
    --services $ECS_SERVICE \
    --region $AWS_REGION

echo -e "${GREEN}🎉 Deployment completed successfully!${NC}"
echo ""
echo -e "${YELLOW}Deployment Details:${NC}"
echo "  ECR Repository: $ECR_URI"
echo "  ECS Cluster: $ECS_CLUSTER"
echo "  ECS Service: $ECS_SERVICE"
echo "  Image Tag: $(git rev-parse --short HEAD)"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "  1. Check ECS service health in AWS Console"
echo "  2. Verify application is responding"
echo "  3. Monitor CloudWatch logs for any issues"
