#!/bin/bash

# Quick fix script to update RDS security group for development access
# WARNING: This opens RDS to the internet - use only for development!

echo "=== Quick Fix for RDS Access ==="
echo "This will add your current IP to the RDS security group"
echo

# Get current public IP
CURRENT_IP=$(curl -s ifconfig.me)
echo "Your current public IP: $CURRENT_IP"

# Get security group ID
SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=healthapp-rds-sg" \
  --query 'SecurityGroups[0].GroupId' \
  --output text 2>/dev/null)

if [ "$SG_ID" = "None" ] || [ -z "$SG_ID" ]; then
    echo "✗ Could not find security group 'healthapp-rds-sg'"
    echo "Please ensure your Terraform infrastructure is deployed"
    exit 1
fi

echo "Found security group: $SG_ID"

# Add ingress rule for current IP
echo "Adding ingress rule for $CURRENT_IP/32..."
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 3306 \
  --cidr $CURRENT_IP/32

if [ $? -eq 0 ]; then
    echo "✓ Successfully added access for your IP"
    echo
    echo "Now try connecting to the database:"
    echo "mysql -h healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com -P 3306 -u admin --ssl-mode=REQUIRED --password=\$(cat /tmp/db_password.txt) -e \"SELECT 1 as test_connection;\""
else
    echo "✗ Failed to add security group rule"
    echo "You may need to run: terraform apply"
fi

echo
echo "=== Alternative Solutions ==="
echo "1. Use AWS Systems Manager Session Manager (recommended for production):"
echo "   aws ssm start-session --target <instance-id>"
echo
echo "2. Deploy the bastion host:"
echo "   terraform apply -target=aws_instance.bastion"
echo
echo "3. Update Terraform and redeploy:"
echo "   terraform apply" 