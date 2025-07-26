#!/bin/bash

# Database connection test script for HealthApp AWS RDS
# Usage: ./test-db-connection.sh

echo "=== HealthApp Database Connection Test ==="
echo

# Database configuration
DB_HOST="healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com"
DB_PORT="3306"
DB_USER="admin"
DB_NAME="healthapp"

# Check if password file exists
if [ -f "/tmp/db_password.txt" ]; then
    DB_PASSWORD=$(cat /tmp/db_password.txt)
    echo "✓ Password file found"
else
    echo "✗ Password file not found at /tmp/db_password.txt"
    echo "Please create the file with your database password"
    exit 1
fi

echo "Testing connection to: $DB_HOST:$DB_PORT"
echo

# Test 1: Basic network connectivity
echo "1. Testing network connectivity..."
if ping -c 1 $DB_HOST > /dev/null 2>&1; then
    echo "✓ Host is reachable"
else
    echo "✗ Host is not reachable"
    echo "   This might be due to security group restrictions"
fi

# Test 2: Port connectivity
echo
echo "2. Testing port connectivity..."
if nc -z -w5 $DB_HOST $DB_PORT 2>/dev/null; then
    echo "✓ Port $DB_PORT is open"
else
    echo "✗ Port $DB_PORT is not accessible"
    echo "   This indicates a security group or network issue"
fi

# Test 3: MySQL connection
echo
echo "3. Testing MySQL connection..."
if mysql -h $DB_HOST -P $DB_PORT -u $DB_USER --ssl-mode=REQUIRED --password="$DB_PASSWORD" -e "SELECT 1 as test_connection;" 2>/dev/null; then
    echo "✓ MySQL connection successful"
else
    echo "✗ MySQL connection failed"
    echo "   Error details:"
    mysql -h $DB_HOST -P $DB_PORT -u $DB_USER --ssl-mode=REQUIRED --password="$DB_PASSWORD" -e "SELECT 1 as test_connection;" 2>&1
fi

# Test 4: Database existence
echo
echo "4. Testing database access..."
if mysql -h $DB_HOST -P $DB_PORT -u $DB_USER --ssl-mode=REQUIRED --password="$DB_PASSWORD" -e "USE $DB_NAME; SHOW TABLES;" 2>/dev/null; then
    echo "✓ Database '$DB_NAME' is accessible"
else
    echo "✗ Cannot access database '$DB_NAME'"
    echo "   Error details:"
    mysql -h $DB_HOST -P $DB_PORT -u $DB_USER --ssl-mode=REQUIRED --password="$DB_PASSWORD" -e "USE $DB_NAME; SHOW TABLES;" 2>&1
fi

echo
echo "=== Troubleshooting Steps ==="
echo "If connection fails, try these steps:"
echo
echo "1. Update security group rules:"
echo "   - Add your IP address to the RDS security group"
echo "   - Or use the bastion host approach"
echo
echo "2. Check AWS RDS status:"
echo "   aws rds describe-db-instances --db-instance-identifier healthapp-db"
echo
echo "3. Verify security group rules:"
echo "   aws ec2 describe-security-groups --filters \"Name=group-name,Values=healthapp-rds-sg\""
echo
echo "4. Use AWS Systems Manager Session Manager:"
echo "   aws ssm start-session --target <instance-id>"
echo "   Then connect to RDS from within the VPC"
echo
echo "5. Check if RDS is publicly accessible:"
echo "   aws rds describe-db-instances --db-instance-identifier healthapp-db --query 'DBInstances[0].PubliclyAccessible'" 