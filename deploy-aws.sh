#!/bin/bash

# HealthApp AWS Deployment Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
AWS_REGION="us-east-1"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_REPOSITORY="healthapp"
ECS_CLUSTER="healthapp-cluster"
ECS_SERVICE="healthapp-service"
IMAGE_TAG="latest"

# Functions
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install it first."
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install it first."
        exit 1
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        print_error "AWS credentials not configured. Please run 'aws configure' first."
        exit 1
    fi
    
    print_success "All prerequisites met!"
}

# Build application
build_application() {
    print_status "Building application with Maven..."
    mvn clean package -DskipTests
    print_success "Application built successfully!"
}

# Build and push Docker image
build_and_push_image() {
    print_status "Building Docker image..."
    docker build -t $ECR_REPOSITORY:$IMAGE_TAG .
    
    print_status "Tagging image for ECR..."
    docker tag $ECR_REPOSITORY:$IMAGE_TAG $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:$IMAGE_TAG
    
    print_status "Logging in to ECR..."
    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
    
    print_status "Pushing image to ECR..."
    docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:$IMAGE_TAG
    
    print_success "Docker image pushed successfully!"
}

# Deploy to ECS
deploy_to_ecs() {
    print_status "Deploying to ECS..."
    
    # Force new deployment
    aws ecs update-service \
        --cluster $ECS_CLUSTER \
        --service $ECS_SERVICE \
        --force-new-deployment \
        --region $AWS_REGION
    
    print_success "Deployment initiated!"
    
    # Wait for deployment to complete
    print_status "Waiting for deployment to complete..."
    aws ecs wait services-stable \
        --cluster $ECS_CLUSTER \
        --services $ECS_SERVICE \
        --region $AWS_REGION
    
    print_success "Deployment completed successfully!"
}

# Get ALB DNS name
get_alb_dns() {
    print_status "Getting ALB DNS name..."
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --names healthapp-alb \
        --region $AWS_REGION \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    print_success "ALB DNS: $ALB_DNS"
    echo $ALB_DNS
}

# Test deployment
test_deployment() {
    print_status "Testing deployment..."
    
    ALB_DNS=$(get_alb_dns)
    
    # Wait for application to be ready
    print_status "Waiting for application to be ready..."
    sleep 60
    
    # Test health endpoint
    if curl -f -s "https://$ALB_DNS/api/actuator/health" > /dev/null; then
        print_success "Health check passed!"
    else
        print_error "Health check failed!"
        exit 1
    fi
    
    # Test API endpoints
    print_status "Testing API endpoints..."
    curl -s "https://$ALB_DNS/api/users" > /dev/null && print_success "Users endpoint working!"
    curl -s "https://$ALB_DNS/api/swagger-ui.html" > /dev/null && print_success "Swagger UI accessible!"
    
    print_success "All tests passed!"
}

# Main deployment function
main() {
    print_status "Starting HealthApp AWS deployment..."
    
    check_prerequisites
    build_application
    build_and_push_image
    deploy_to_ecs
    test_deployment
    
    ALB_DNS=$(get_alb_dns)
    
    print_success "🎉 Deployment completed successfully!"
    echo ""
    echo "🌐 Application URLs:"
    echo "   API Base: https://$ALB_DNS/api"
    echo "   Swagger UI: https://$ALB_DNS/api/swagger-ui.html"
    echo "   Health Check: https://$ALB_DNS/api/actuator/health"
    echo ""
    echo "📊 Monitoring:"
    echo "   ECS Console: https://console.aws.amazon.com/ecs/home?region=$AWS_REGION#/clusters/$ECS_CLUSTER"
    echo "   CloudWatch: https://console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION#logsV2:log-groups/log-group/ecs/healthapp"
}

# Run main function
main "$@" 