#!/bin/bash

# HealthApp Infrastructure Monitoring Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Check VPC
check_vpc() {
    print_status "Checking VPC..."
    if aws ec2 describe-vpcs --filters "Name=tag:Name,Values=healthapp-vpc" --query 'Vpcs[0].State' --output text 2>/dev/null | grep -q "available"; then
        print_success "VPC created successfully"
        return 0
    else
        print_warning "VPC not ready yet"
        return 1
    fi
}

# Check ECR Repository
check_ecr() {
    print_status "Checking ECR Repository..."
    if aws ecr describe-repositories --repository-names healthapp --query 'repositories[0].repositoryName' --output text 2>/dev/null | grep -q "healthapp"; then
        print_success "ECR repository created successfully"
        return 0
    else
        print_warning "ECR repository not ready yet"
        return 1
    fi
}

# Check RDS Database
check_rds() {
    print_status "Checking RDS Database..."
    STATUS=$(aws rds describe-db-instances --db-instance-identifier healthapp-db --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null)
    if [ "$STATUS" = "available" ]; then
        print_success "RDS database created successfully"
        return 0
    elif [ "$STATUS" = "creating" ]; then
        print_warning "RDS database is still being created (Status: $STATUS)"
        return 1
    else
        print_warning "RDS database not ready yet (Status: $STATUS)"
        return 1
    fi
}

# Check Load Balancer
check_alb() {
    print_status "Checking Application Load Balancer..."
    if aws elbv2 describe-load-balancers --names healthapp-alb --query 'LoadBalancers[0].State.Code' --output text 2>/dev/null | grep -q "active"; then
        print_success "ALB created successfully"
        return 0
    else
        print_warning "ALB not ready yet"
        return 1
    fi
}

# Check ECS Cluster
check_ecs() {
    print_status "Checking ECS Cluster..."
    if aws ecs describe-clusters --clusters healthapp-cluster --query 'clusters[0].status' --output text 2>/dev/null | grep -q "ACTIVE"; then
        print_success "ECS cluster created successfully"
        return 0
    else
        print_warning "ECS cluster not ready yet"
        return 1
    fi
}

# Check Secrets Manager
check_secrets() {
    print_status "Checking Secrets Manager..."
    DB_SECRET=$(aws secretsmanager describe-secret --secret-id healthapp/db-password --query 'Name' --output text 2>/dev/null)
    JWT_SECRET=$(aws secretsmanager describe-secret --secret-id healthapp/jwt-secret --query 'Name' --output text 2>/dev/null)
    
    if [ "$DB_SECRET" = "healthapp/db-password" ] && [ "$JWT_SECRET" = "healthapp/jwt-secret" ]; then
        print_success "Secrets created successfully"
        return 0
    else
        print_warning "Secrets not ready yet"
        return 1
    fi
}

# Main monitoring function
main() {
    print_status "Starting HealthApp infrastructure monitoring..."
    echo ""
    
    # Initialize counters
    TOTAL_RESOURCES=6
    CREATED_RESOURCES=0
    
    # Check each resource
    check_vpc && ((CREATED_RESOURCES++))
    check_ecr && ((CREATED_RESOURCES++))
    check_rds && ((CREATED_RESOURCES++))
    check_alb && ((CREATED_RESOURCES++))
    check_ecs && ((CREATED_RESOURCES++))
    check_secrets && ((CREATED_RESOURCES++))
    
    echo ""
    print_status "Progress: $CREATED_RESOURCES/$TOTAL_RESOURCES resources created"
    
    if [ $CREATED_RESOURCES -eq $TOTAL_RESOURCES ]; then
        print_success "🎉 All infrastructure resources are ready!"
        echo ""
        print_status "Next steps:"
        echo "1. Add GitHub secrets (if not done already)"
        echo "2. Push a change to trigger the first deployment"
        echo "3. Monitor the deployment in GitHub Actions"
    else
        print_warning "Some resources are still being created. This is normal - RDS can take 5-10 minutes."
        echo ""
        print_status "You can continue monitoring with: ./monitor-deployment.sh"
    fi
}

# Run main function
main "$@" 