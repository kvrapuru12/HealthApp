# AWS Database Connection Guide

## Current Database Details

**Endpoint:** `healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com`  
**Port:** `3306`  
**Database:** `healthapp`  
**Username:** `admin`  
**Password:** `HealthApp2024!SecurePassword123`  
**Region:** `us-east-1`

## Connection Methods

### Method 1: Direct MySQL Connection (If Network Allows)

```bash
mysql -h healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com \
  -P 3306 \
  -u admin \
  --ssl-mode=REQUIRED \
  --password='HealthApp2024!SecurePassword123' \
  healthapp
```

### Method 2: Using the Generated Scripts

```bash
# Test connection
./test-db-connection.sh

# Connect to database
./connect-mysql.sh
```

### Method 3: Using MySQL Configuration File

The script created `~/.my.cnf` with your connection details:

```bash
mysql
```

### Method 4: Environment Variables

```bash
export DB_HOST="healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com"
export DB_PORT="3306"
export DB_NAME="healthapp"
export DB_USERNAME="admin"
export DB_PASSWORD="HealthApp2024!SecurePassword123"

mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME --ssl-mode=REQUIRED --password="$DB_PASSWORD" $DB_NAME
```

## Application Connection

### For Your Spring Boot Application

Use these properties in your `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com:3306/healthapp?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=admin
spring.datasource.password=HealthApp2024!SecurePassword123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### For Local Development

Create `src/main/resources/application-local.properties`:

```properties
# Local development profile
spring.profiles.active=local

# Database Configuration
spring.datasource.url=jdbc:mysql://healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com:3306/healthapp?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=admin
spring.datasource.password=HealthApp2024!SecurePassword123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Enable SQL logging for debugging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

Run your application with:
```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

## Troubleshooting Connection Issues

### Issue: Network Connectivity Blocked

If you're getting connection timeouts, try these solutions:

#### 1. Check Your Network
- Are you on a corporate network with firewall restrictions?
- Try connecting from a different network (mobile hotspot)
- Check if your ISP is blocking port 3306

#### 2. Use AWS Systems Manager Session Manager (Recommended)

This is the most secure way to connect to your database:

```bash
# First, find an EC2 instance in your VPC
aws ec2 describe-instances \
  --filters "Name=vpc-id,Values=vpc-xxxxx" \
  --query 'Reservations[].Instances[?State.Name==`running`].[InstanceId]' \
  --output text

# Start a session (replace with your instance ID)
aws ssm start-session --target i-1234567890abcdef0

# From within the session, connect to your database
mysql -h healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com \
  -P 3306 \
  -u admin \
  --ssl-mode=REQUIRED \
  --password='HealthApp2024!SecurePassword123' \
  healthapp
```

#### 3. Set Up a Bastion Host

Create an EC2 instance in your VPC and use it as a jump server:

```bash
# SSH to your bastion host
ssh -i your-key.pem ec2-user@your-bastion-host

# From bastion host, connect to RDS
mysql -h healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com \
  -P 3306 \
  -u admin \
  --ssl-mode=REQUIRED \
  --password='HealthApp2024!SecurePassword123' \
  healthapp
```

#### 4. Use SSH Tunnel

If you have a bastion host, create an SSH tunnel:

```bash
# Create SSH tunnel
ssh -i your-key.pem -L 3307:healthapp-db.cg3mu4uec4gj.us-east-1.rds.amazonaws.com:3306 ec2-user@your-bastion-host

# In another terminal, connect to localhost:3307
mysql -h 127.0.0.1 -P 3307 -u admin --ssl-mode=REQUIRED --password='HealthApp2024!SecurePassword123' healthapp
```

#### 5. Use AWS VPN or Direct Connect

For production environments, consider:
- AWS Client VPN
- AWS Site-to-Site VPN
- AWS Direct Connect

## Database Management

### View Tables
```sql
USE healthapp;
SHOW TABLES;
```

### Check Flyway Migration Status
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### View User Data
```sql
SELECT * FROM users LIMIT 10;
SELECT * FROM activity_entries LIMIT 10;
SELECT * FROM food_entries LIMIT 10;
```

### Check Database Size
```sql
SELECT 
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables 
WHERE table_schema = 'healthapp'
GROUP BY table_schema;
```

## Security Best Practices

1. **Never commit passwords to version control**
2. **Use AWS Secrets Manager for production passwords**
3. **Limit security group access to specific IP ranges**
4. **Use SSL/TLS for all connections**
5. **Consider using IAM database authentication**
6. **Regularly rotate database passwords**

## Monitoring and Logging

### Check RDS Logs
```bash
aws rds describe-db-log-files \
  --db-instance-identifier healthapp-db \
  --region us-east-1
```

### Monitor Database Metrics
```bash
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name CPUUtilization \
  --dimensions Name=DBInstanceIdentifier,Value=healthapp-db \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average
```

## Quick Commands Reference

```bash
# Test connection
./test-db-connection.sh

# Connect to database
./connect-mysql.sh

# Fix security group access
./fix-db-access.sh

# Get database info
./connect-aws-db.sh
```

## Support

If you continue to have connection issues:

1. Check AWS RDS console for instance status
2. Verify security group rules in AWS console
3. Test from an EC2 instance in the same VPC
4. Contact your network administrator for firewall restrictions
5. Consider using AWS Support if you have a support plan 