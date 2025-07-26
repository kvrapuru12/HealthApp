#!/bin/bash

# HealthApp AWS Database Security Group Fix Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Configuration
AWS_REGION="us-east-1"
DB_INSTANCE_ID="healthapp-db"

print_status "Getting database security group information..."

# Get the security group ID
SECURITY_GROUP_ID=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $AWS_REGION \
    --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' \
    --output text)

if [ -z "$SECURITY_GROUP_ID" ] || [ "$SECURITY_GROUP_ID" == "None" ]; then
    print_error "Could not retrieve security group ID."
    exit 1
fi

print_success "Security Group ID: $SECURITY_GROUP_ID"

# Get current IP address
CURRENT_IP=$(curl -s https://checkip.amazonaws.com/)
print_status "Your current IP address: $CURRENT_IP"

# Check current security group rules
print_status "Checking current security group rules..."
aws ec2 describe-security-groups \
    --group-ids $SECURITY_GROUP_ID \
    --region $AWS_REGION \
    --query 'SecurityGroups[0].IpPermissions[?FromPort==`3306`]' \
    --output table

echo ""
print_status "Adding your IP address to the security group..."

# Add your IP to the security group
aws ec2 authorize-security-group-ingress \
    --group-id $SECURITY_GROUP_ID \
    --protocol tcp \
    --port 3306 \
    --cidr $CURRENT_IP/32 \
    --region $AWS_REGION

print_success "Added your IP ($CURRENT_IP) to the security group!"

echo ""
print_warning "Security Note:"
echo "   - This allows access from your current IP address only"
echo "   - If your IP changes, you'll need to run this script again"
echo "   - For production, consider using VPN or bastion hosts"
echo ""

# Test connection again
print_status "Testing database connection..."
sleep 5

if ./connect-mysql.sh; then
    print_success "Database connection successful!"
else
    print_error "Connection still failed. Please check:"
    echo "   1. Your internet connection"
    echo "   2. AWS credentials"
    echo "   3. Database instance status"
fi 