# HealthApp AWS Deployment & CI/CD Guide

Complete guide for deploying HealthApp to AWS and setting up CI/CD pipelines.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [MySQL Database Setup](#mysql-database-setup)
   - [Local Development Setup](#local-development-setup)
   - [AWS RDS MySQL Setup](#aws-rds-mysql-setup)
4. [Infrastructure Setup (Terraform)](#infrastructure-setup-terraform)
5. [GitHub Secrets Configuration](#github-secrets-configuration)
6. [CI/CD Pipeline Setup](#cicd-pipeline-setup)
7. [Manual Deployment](#manual-deployment)
8. [Monitoring & Logs](#monitoring--logs)
9. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
Internet → ALB → ECS Fargate → RDS MySQL
                ↓
            ECR Repository
```

**Components:**
- **VPC**: Custom VPC with public and private subnets across 2 AZs
- **RDS**: MySQL database in private subnets
- **ECS Fargate**: Containerized Spring Boot application
- **ALB**: Application Load Balancer for traffic distribution
- **ECR**: Container registry
- **CloudWatch**: Logging and monitoring
- **Secrets Manager**: Secure storage for sensitive data

---

## Prerequisites

- AWS CLI configured with appropriate credentials
- Terraform >= 1.0 installed
- Docker installed
- GitHub repository
- Appropriate AWS IAM permissions
- **MySQL 8.0+** (for local development)

---

## MySQL Database Setup

### Local Development Setup

For local development, you need to set up MySQL on your machine:

#### 1. Install MySQL

**macOS (using Homebrew):**
```bash
brew install mysql
brew services start mysql
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

**Windows:**
- Download and install from [MySQL Official Website](https://dev.mysql.com/downloads/mysql/)

#### 2. Create Database and User

Connect to MySQL and create the database:

```bash
mysql -u root -p
```

Then run:
```sql
-- Create database
CREATE DATABASE healthapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional - you can use root for local dev)
CREATE USER 'healthapp_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON healthapp.* TO 'healthapp_user'@'localhost';
FLUSH PRIVILEGES;

-- Verify
SHOW DATABASES;
USE healthapp;
```

#### 3. Configure Application Properties

Update `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/healthapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

Or use environment variables:
```bash
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
```

#### 4. Database Migrations

The application uses **Flyway** for database migrations. When you start the application, Flyway automatically:
- Creates all required tables
- Runs all migration scripts from `src/main/resources/db/migration/`
- Versions the database schema

No manual SQL execution needed - just start the app!

```bash
mvn spring-boot:run
```

### AWS RDS MySQL Setup

For AWS deployment, **RDS MySQL is automatically created by Terraform**. Here's what you need to know:

#### Database Configuration (via Terraform)

The RDS instance is configured with:
- **Engine**: MySQL 8.0.35
- **Instance Class**: db.t3.micro (configurable)
- **Database Name**: `healthapp` (created automatically)
- **Username**: `admin` (from Terraform variables)
- **Password**: Set via `db_password` in `terraform.tfvars`
- **Storage**: 20GB gp2 (configurable)
- **Backups**: Enabled (7 days retention in production)

#### Accessing RDS After Deployment

1. Get the RDS endpoint from Terraform:
```bash
cd terraform
terraform output rds_endpoint
```

2. The database is accessible from:
   - ECS tasks in the private subnet (automatic)
   - Your local machine (if RDS is publicly accessible in dev/staging)
   - Via VPN/Bastion host in production

3. Connection details are stored in AWS Secrets Manager:
   - Password: `healthapp/db-password`
   - Available to ECS tasks via environment variables

#### Database Migrations on AWS

Flyway migrations run **automatically on application startup** in ECS:
- All migrations in `src/main/resources/db/migration/` are executed
- Migrations are idempotent (safe to run multiple times)
- Schema version is tracked in `flyway_schema_history` table

**Important Notes:**
- Migrations run automatically - no manual SQL needed
- Ensure database user has CREATE, ALTER, DROP permissions
- In production, test migrations in staging first

---

## Infrastructure Setup (Terraform)

### Step 1: Configure Variables

1. Copy the example variables file:
```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

2. Edit `terraform.tfvars` with your values:
```hcl
aws_region = "us-east-1"
db_password = "YOUR_SECURE_PASSWORD"
jwt_secret = "YOUR_JWT_SECRET"
environment = "production"  # or "staging" or "development"
```

**⚠️ Important**: Never commit `terraform.tfvars` to version control if it contains real secrets!

### Step 2: Configure Remote State (Recommended for Production)

For team collaboration and state backup:

1. Create S3 bucket for Terraform state:
```bash
aws s3 mb s3://healthapp-terraform-state --region us-east-1
aws s3api put-bucket-versioning --bucket healthapp-terraform-state --versioning-configuration Status=Enabled
```

2. Create DynamoDB table for state locking:
```bash
aws dynamodb create-table \
  --table-name terraform-state-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
  --region us-east-1
```

3. Uncomment and configure backend in `terraform/main.tf`:
```hcl
backend "s3" {
  bucket         = "healthapp-terraform-state"
  key            = "terraform.tfstate"
  region         = "us-east-1"
  encrypt        = true
  dynamodb_table = "terraform-state-lock"
}
```

### Step 3: Deploy Infrastructure

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

### Step 4: Get Deployment Outputs

After successful deployment, get important values:

```bash
terraform output
```

Key outputs:
- `alb_dns_name`: Application URL (e.g., `http://healthapp-alb-xxx.us-east-1.elb.amazonaws.com`)
- `rds_endpoint`: Database endpoint
- `ecr_repository_url`: Container registry URL

### Terraform Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `aws_region` | AWS region | `us-east-1` | No |
| `db_password` | Database password (min 8 chars) | - | Yes |
| `jwt_secret` | JWT secret key | `null` | No |
| `environment` | Environment (production/staging/development) | `production` | No |
| `app_name` | Application name | `healthapp` | No |
| `ecs_desired_count` | Desired ECS task count | Auto (1 for dev, 2 for prod) | No |
| `enable_rds_backups` | Enable RDS automated backups | `true` | No |
| `rds_backup_retention_days` | RDS backup retention days | `7` | No |

### Security Features

- ✅ RDS in private subnets (publicly accessible only in non-production)
- ✅ Security groups with least privilege access
- ✅ Secrets stored in AWS Secrets Manager
- ✅ Encryption at rest for RDS
- ✅ IAM roles with minimal permissions
- ✅ CloudWatch alarms for monitoring

### Cost Optimizations

- Environment-based resource scaling (1 task for dev, 2 for production)
- ECR lifecycle policy (keeps last 10 images, deletes older ones)
- Configurable backup retention
- Conditional deletion protection for production

---

## GitHub Secrets Configuration

### Required Secrets

For CI/CD pipeline to work, configure these secrets in GitHub:

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** and add:

#### AWS_ACCESS_KEY_ID
- **Purpose**: AWS access key for authentication
- **Format**: `AKIA...` (20 characters)
- **Permissions**: Should have ECR, ECS permissions

#### AWS_SECRET_ACCESS_KEY
- **Purpose**: AWS secret key for authentication
- **Format**: Secret string (40 characters)
- **Security**: Keep this highly secure

### AWS IAM User Setup

1. Go to AWS IAM Console
2. Create a new user with programmatic access
3. Attach policies or create custom policy:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage",
                "ecr:PutImage",
                "ecr:InitiateLayerUpload",
                "ecr:UploadLayerPart",
                "ecr:CompleteLayerUpload"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "ecs:DescribeTaskDefinition",
                "ecs:RegisterTaskDefinition",
                "ecs:UpdateService",
                "ecs:DescribeServices"
            ],
            "Resource": "*"
        }
    ]
}
```

**Security Best Practices:**
- Use least privilege principle
- Rotate access keys regularly
- Monitor AWS CloudTrail for access logs
- Never commit secrets to code

---

## CI/CD Pipeline Setup

### Pipeline Overview

The CI/CD pipeline consists of GitHub Actions workflows:

1. **Test Stage**: Runs on every push and pull request
   - Executes unit tests
   - Builds the application

2. **Build & Deploy Stage**: Only runs on main branch pushes
   - Builds Docker image
   - Pushes to Amazon ECR
   - Deploys to ECS

### Automated Deployment

The pipeline automatically deploys when you push to the main branch:

```bash
git push origin main
```

Monitor deployment progress:
- Check **GitHub Actions** tab for pipeline status
- Monitor ECS service health in AWS Console
- Check application logs in CloudWatch

### Workflow Files

- `.github/workflows/deploy.yml` - Main deployment pipeline
- `.github/workflows/aws-deploy.yml` - AWS-specific deployment

---

## Manual Deployment

If you need to deploy manually:

### 1. Build and Push Docker Image

```bash
# Get ECR login token
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build Docker image
docker build -t healthapp .

# Tag for ECR (replace with your ECR URL from terraform output)
docker tag healthapp:latest <ecr-repository-url>:latest

# Push to ECR
docker push <ecr-repository-url>:latest
```

### 2. Deploy to ECS

```bash
# Force new deployment (pulls latest image)
aws ecs update-service \
  --cluster healthapp-cluster \
  --service healthapp-service \
  --force-new-deployment \
  --region us-east-1
```

---

## Monitoring & Logs

### CloudWatch Logs

ECS tasks automatically send logs to CloudWatch:

```bash
# View logs
aws logs tail /ecs/healthapp --follow --region us-east-1
```

### Health Checks

The application exposes health endpoints:

- **Health Check**: `http://<alb-dns>/api/actuator/health`
- **Swagger UI**: `http://<alb-dns>/api/swagger-ui.html`
- **API Base**: `http://<alb-dns>/api`

### CloudWatch Alarms

The infrastructure includes 6 CloudWatch alarms:

1. **ECS CPU Utilization** - Alerts when CPU > 80%
2. **ECS Memory Utilization** - Alerts when Memory > 80%
3. **ALB Response Time** - Alerts when response time > 5 seconds
4. **ALB 5xx Errors** - Alerts when 5xx errors exceed threshold
5. **RDS CPU Utilization** - Alerts when RDS CPU > 80%
6. **RDS Database Connections** - Alerts when connections are high

**Note**: Configure SNS topics and email subscriptions for alarm notifications.

---

## Troubleshooting

### Database Connection Issues

1. **Check security groups**
   - Verify RDS security group allows MySQL (3306) from ECS security group
   - Check security group IDs match

2. **Verify RDS endpoint**
   - Get endpoint from Terraform output: `terraform output rds_endpoint`
   - Ensure it's accessible from ECS tasks

3. **Check credentials**
   - Verify secrets in AWS Secrets Manager
   - Check ECS task definition secrets configuration

### Container/ECS Issues

1. **Check ECS task logs**
   ```bash
   aws logs tail /ecs/healthapp --follow
   ```

2. **Verify image exists in ECR**
   - Check ECR repository has images
   - Verify image tag matches task definition

3. **Check task definition**
   - Verify task definition is registered
   - Check CPU/memory allocation
   - Verify environment variables and secrets

### Load Balancer Issues

1. **Verify target group health**
   - Check ALB target group health checks
   - Ensure health check path is correct: `/api/actuator/health`

2. **Check security groups**
   - Verify ALB security group allows HTTP (80) from internet
   - Verify ECS security group allows 8080 from ALB

3. **Verify listener rules**
   - Check ALB listener configuration
   - Ensure default action forwards to target group

### Common Error Messages

**"Task failed to start"**
- Check CloudWatch logs for container errors
- Verify image exists and is accessible
- Check task definition resource limits

**"Health check failed"**
- Verify application is listening on port 8080
- Check health endpoint responds with 200
- Review application startup logs

**"Database connection timeout"**
- Verify RDS security group rules
- Check RDS endpoint is correct
- Ensure database is accessible from private subnet

---

## Application Configuration

### Environment Variables

The application uses these environment variables (configured in ECS task definition):

- `SPRING_PROFILES_ACTIVE`: Set to `aws`
- `DB_HOST`: RDS endpoint (from Terraform output)
- `DB_USERNAME`: `admin`
- `DB_NAME`: `healthapp`
- `DB_PORT`: `3306`
- `SPRING_DATASOURCE_PASSWORD`: From AWS Secrets Manager
- `JWT_SECRET`: From AWS Secrets Manager

### Database Migration

The application uses Flyway for database migrations:
- **Migrations run automatically** on application startup
- Located in `src/main/resources/db/migration/`
- Numbered sequentially (V1__, V2__, etc.)
- **No manual SQL execution needed** - Flyway handles everything
- Schema version tracked in `flyway_schema_history` table

**Migration files location:** `src/main/resources/db/migration/`

---

## Scaling

### Manual Scaling

```bash
# Update desired count
aws ecs update-service \
  --cluster healthapp-cluster \
  --service healthapp-service \
  --desired-count 4 \
  --region us-east-1
```

### Auto Scaling (Optional)

Configure ECS service auto-scaling:

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/healthapp-cluster/healthapp-service \
  --min-capacity 1 \
  --max-capacity 10 \
  --region us-east-1

# Create scaling policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/healthapp-cluster/healthapp-service \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    }
  }' \
  --region us-east-1
```

---

## Cleanup

To destroy all infrastructure:

```bash
cd terraform
terraform destroy
```

**⚠️ Warning**: This will delete all infrastructure including the database and all data!

---

## Additional Resources

- **AWS Console**: https://console.aws.amazon.com
- **Terraform Docs**: https://www.terraform.io/docs
- **ECS Documentation**: https://docs.aws.amazon.com/ecs
- **RDS Documentation**: https://docs.aws.amazon.com/rds

---

## Quick Reference

### Useful Commands

```bash
# View Terraform outputs
cd terraform && terraform output

# Check ECS service status
aws ecs describe-services --cluster healthapp-cluster --services healthapp-service

# View CloudWatch logs
aws logs tail /ecs/healthapp --follow

# Get ECR login
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Force ECS service update
aws ecs update-service --cluster healthapp-cluster --service healthapp-service --force-new-deployment
```

