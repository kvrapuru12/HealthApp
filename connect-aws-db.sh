#!/bin/bash

# HealthApp AWS Database Connection Script
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
SECRET_ID="healthapp/db-password"

print_status "Getting AWS RDS database information..."

# Get database endpoint
DB_ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $AWS_REGION \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text)

if [ -z "$DB_ENDPOINT" ] || [ "$DB_ENDPOINT" == "None" ]; then
    print_error "Could not retrieve database endpoint. Please check your AWS credentials and region."
    exit 1
fi

# Get database port
DB_PORT=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $AWS_REGION \
    --query 'DBInstances[0].Endpoint.Port' \
    --output text)

# Get database name
DB_NAME=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $AWS_REGION \
    --query 'DBInstances[0].DBName' \
    --output text)

# Get database username
DB_USERNAME=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $AWS_REGION \
    --query 'DBInstances[0].MasterUsername' \
    --output text)

# Get database password from Secrets Manager
print_status "Retrieving database password from AWS Secrets Manager..."
DB_PASSWORD=$(aws secretsmanager get-secret-value \
    --secret-id $SECRET_ID \
    --region $AWS_REGION \
    --query 'SecretString' \
    --output text)

if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" == "None" ]; then
    print_error "Could not retrieve database password from Secrets Manager."
    exit 1
fi

# Check if database is publicly accessible
PUBLICLY_ACCESSIBLE=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $AWS_REGION \
    --query 'DBInstances[0].PubliclyAccessible' \
    --output text)

print_success "Database information retrieved successfully!"
echo ""
echo "📊 Database Connection Details:"
echo "   Endpoint: $DB_ENDPOINT"
echo "   Port: $DB_PORT"
echo "   Database: $DB_NAME"
echo "   Username: $DB_USERNAME"
echo "   Publicly Accessible: $PUBLICLY_ACCESSIBLE"
echo ""

if [ "$PUBLICLY_ACCESSIBLE" == "False" ]; then
    print_warning "Database is NOT publicly accessible. You may need to:"
    echo "   1. Use AWS VPN or Direct Connect"
    echo "   2. Connect from an EC2 instance in the same VPC"
    echo "   3. Use AWS Systems Manager Session Manager"
    echo "   4. Configure a bastion host"
    echo ""
fi

# Test connection
print_status "Testing database connection..."
if command -v mysql &> /dev/null; then
    if mysql -h "$DB_ENDPOINT" -P "$DB_PORT" -u "$DB_USERNAME" --ssl-mode=REQUIRED --password="$DB_PASSWORD" -e "SELECT 1;" 2>/dev/null; then
        print_success "Database connection successful!"
    else
        print_error "Database connection failed. Please check your network connectivity and security groups."
    fi
else
    print_warning "MySQL client not found. Install it to test the connection."
fi

echo ""
echo "🔧 Connection Methods:"
echo ""
echo "1. MySQL Command Line:"
echo "   mysql -h $DB_ENDPOINT -P $DB_PORT -u $DB_USERNAME --ssl-mode=REQUIRED --password='$DB_PASSWORD' $DB_NAME"
echo ""
echo "2. JDBC URL (for applications):"
echo "   jdbc:mysql://$DB_ENDPOINT:$DB_PORT/$DB_NAME?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true"
echo ""
echo "3. Environment Variables:"
echo "   export DB_HOST=$DB_ENDPOINT"
echo "   export DB_PORT=$DB_PORT"
echo "   export DB_NAME=$DB_NAME"
echo "   export DB_USERNAME=$DB_USERNAME"
echo "   export DB_PASSWORD=$DB_PASSWORD"
echo ""
echo "4. Application Properties:"
echo "   spring.datasource.url=jdbc:mysql://$DB_ENDPOINT:$DB_PORT/$DB_NAME?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true"
echo "   spring.datasource.username=$DB_USERNAME"
echo "   spring.datasource.password=$DB_PASSWORD"
echo ""

# Create connection script
cat > connect-mysql.sh << EOF
#!/bin/bash
mysql -h $DB_ENDPOINT -P $DB_PORT -u $DB_USERNAME --ssl-mode=REQUIRED --password="$DB_PASSWORD" $DB_NAME
EOF

chmod +x connect-mysql.sh
print_success "Created 'connect-mysql.sh' script for easy connection!"
echo "   Run: ./connect-mysql.sh" 