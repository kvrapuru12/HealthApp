# AWS Deployment Guide

Complete guide to deploy HealthApp to AWS infrastructure.

## 🏗️ AWS Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Route 53      │    │   Application   │    │   RDS MySQL     │
│   (DNS)         │───▶│   Load Balancer │───▶│   Database      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   ECS Fargate   │
                       │   (Container)   │
                       └─────────────────┘
```

## 📋 Prerequisites

- **AWS CLI** installed and configured
- **Docker** installed locally
- **AWS Account** with appropriate permissions
- **Domain name** (optional, for custom domain)

## 🔧 Step 1: AWS Infrastructure Setup

### 1.1 Create VPC and Networking

```bash
# Create VPC
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --tag-specifications ResourceType=vpc,Tags=[{Key=Name,Value=healthapp-vpc}]

# Create subnets
aws ec2 create-subnet \
  --vpc-id vpc-xxxxxxxxx \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications ResourceType=subnet,Tags=[{Key=Name,Value=healthapp-public-1a}]

aws ec2 create-subnet \
  --vpc-id vpc-xxxxxxxxx \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications ResourceType=subnet,Tags=[{Key=Name,Value=healthapp-public-1b}]

# Create Internet Gateway
aws ec2 create-internet-gateway \
  --tag-specifications ResourceType=internet-gateway,Tags=[{Key=Name,Value=healthapp-igw}]

# Attach Internet Gateway to VPC
aws ec2 attach-internet-gateway \
  --vpc-id vpc-xxxxxxxxx \
  --internet-gateway-id igw-xxxxxxxxx
```

### 1.2 Create RDS MySQL Database

```bash
# Create DB Subnet Group
aws rds create-db-subnet-group \
  --db-subnet-group-name healthapp-db-subnet \
  --db-subnet-group-description "HealthApp Database Subnet Group" \
  --subnet-ids subnet-xxxxxxxxx subnet-yyyyyyyyy

# Create Security Group for RDS
aws ec2 create-security-group \
  --group-name healthapp-db-sg \
  --description "HealthApp Database Security Group" \
  --vpc-id vpc-xxxxxxxxx

# Add rule to allow ECS access to RDS
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxxx \
  --protocol tcp \
  --port 3306 \
  --source-group sg-xxxxxxxxx

# Create RDS Instance
aws rds create-db-instance \
  --db-instance-identifier healthapp-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0.35 \
  --master-username admin \
  --master-user-password YourSecurePassword123! \
  --allocated-storage 20 \
  --storage-type gp2 \
  --db-subnet-group-name healthapp-db-subnet \
  --vpc-security-group-ids sg-xxxxxxxxx \
  --backup-retention-period 7 \
  --multi-az false \
  --publicly-accessible false \
  --storage-encrypted true
```

### 1.3 Create Application Load Balancer

```bash
# Create Security Group for ALB
aws ec2 create-security-group \
  --group-name healthapp-alb-sg \
  --description "HealthApp ALB Security Group" \
  --vpc-id vpc-xxxxxxxxx

# Allow HTTP and HTTPS traffic
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxxx \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxxx \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

# Create ALB
aws elbv2 create-load-balancer \
  --name healthapp-alb \
  --subnets subnet-xxxxxxxxx subnet-yyyyyyyyy \
  --security-groups sg-xxxxxxxxx \
  --scheme internet-facing \
  --type application
```

## 🐳 Step 2: Docker Configuration

### 2.1 Create Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/healthapp-backend-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2.2 Create .dockerignore

```
target/
!target/*.jar
.git/
.gitignore
README.md
*.md
```

## 🚀 Step 3: ECS Setup

### 3.1 Create ECR Repository

```bash
# Create ECR repository
aws ecr create-repository \
  --repository-name healthapp \
  --image-scanning-configuration scanOnPush=true

# Get login token
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com
```

### 3.2 Build and Push Docker Image

```bash
# Build application
mvn clean package -DskipTests

# Build Docker image
docker build -t healthapp .

# Tag image
docker tag healthapp:latest $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/healthapp:latest

# Push to ECR
docker push $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/healthapp:latest
```

### 3.3 Create ECS Cluster

```bash
# Create ECS cluster
aws ecs create-cluster \
  --cluster-name healthapp-cluster \
  --capacity-providers FARGATE \
  --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1
```

### 3.4 Create Task Definition

```json
{
  "family": "healthapp-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::$AWS_ACCOUNT_ID:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "healthapp",
      "image": "$AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/healthapp:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "aws"
        }
      ],
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:$AWS_ACCOUNT_ID:secret:healthapp/db-password"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:$AWS_ACCOUNT_ID:secret:healthapp/jwt-secret"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/healthapp",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### 3.5 Create ECS Service

```bash
# Create ECS service
aws ecs create-service \
  --cluster healthapp-cluster \
  --service-name healthapp-service \
  --task-definition healthapp-task:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxxxxxx,subnet-yyyyyyyyy],securityGroups=[sg-xxxxxxxxx],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:us-east-1:$AWS_ACCOUNT_ID:targetgroup/healthapp-tg,containerName=healthapp,containerPort=8080"
```

## 🔐 Step 4: Security and Secrets

### 4.1 Create Secrets Manager

```bash
# Store database password
aws secretsmanager create-secret \
  --name healthapp/db-password \
  --description "HealthApp Database Password" \
  --secret-string "YourSecurePassword123!"

# Store JWT secret
aws secretsmanager create-secret \
  --name healthapp/jwt-secret \
  --description "HealthApp JWT Secret" \
  --secret-string "$(openssl rand -hex 32)"
```

### 4.2 Create IAM Roles

```bash
# Create ECS task execution role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "ecs-tasks.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  }'

# Attach policies
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
```

## 🌐 Step 5: DNS and SSL (Optional)

### 5.1 Route 53 Setup

```bash
# Create hosted zone (if you have a domain)
aws route53 create-hosted-zone \
  --name yourdomain.com \
  --caller-reference $(date +%s)

# Create A record
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890 \
  --change-batch '{
    "Changes": [
      {
        "Action": "CREATE",
        "ResourceRecordSet": {
          "Name": "api.yourdomain.com",
          "Type": "A",
          "AliasTarget": {
            "HostedZoneId": "Z35SXDOTRQ7X7K",
            "DNSName": "your-alb-dns-name.us-east-1.elb.amazonaws.com",
            "EvaluateTargetHealth": true
          }
        }
      }
    ]
  }'
```

### 5.2 SSL Certificate

```bash
# Request SSL certificate
aws acm request-certificate \
  --domain-name api.yourdomain.com \
  --validation-method DNS \
  --subject-alternative-names "*.yourdomain.com"
```

## 🧪 Step 6: Testing and Verification

### 6.1 Health Check

```bash
# Get ALB DNS name
ALB_DNS=$(aws elbv2 describe-load-balancers --names healthapp-alb --query 'LoadBalancers[0].DNSName' --output text)

# Test health endpoint
curl -f https://$ALB_DNS/api/actuator/health
```

### 6.2 API Testing

```bash
# Test API endpoints
curl https://$ALB_DNS/api/users
curl https://$ALB_DNS/api/swagger-ui.html
```

## 📊 Step 6: Monitoring and Logging

### 6.1 CloudWatch Logs

```bash
# Create log group
aws logs create-log-group --log-group-name /ecs/healthapp

# Set retention policy
aws logs put-retention-policy \
  --log-group-name /ecs/healthapp \
  --retention-in-days 30
```

### 6.2 CloudWatch Alarms

```bash
# Create CPU utilization alarm
aws cloudwatch put-metric-alarm \
  --alarm-name healthapp-cpu-high \
  --alarm-description "High CPU utilization" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --alarm-actions arn:aws:sns:us-east-1:$AWS_ACCOUNT_ID:healthapp-alerts
```

## 🎉 Success!

Your HealthApp is now deployed to AWS with:
- ✅ **Scalable infrastructure** with ECS Fargate
- ✅ **Managed database** with RDS MySQL
- ✅ **Load balancing** with Application Load Balancer
- ✅ **Security** with Secrets Manager and IAM
- ✅ **Monitoring** with CloudWatch
- ✅ **SSL/TLS** encryption (if domain configured)

## 🔧 Maintenance

### Update Application

```bash
# Build new image
mvn clean package -DskipTests
docker build -t healthapp .
docker tag healthapp:latest $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/healthapp:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/healthapp:latest

# Update ECS service
aws ecs update-service \
  --cluster healthapp-cluster \
  --service healthapp-service \
  --force-new-deployment
```

### Scale Application

```bash
# Scale up
aws ecs update-service \
  --cluster healthapp-cluster \
  --service healthapp-service \
  --desired-count 4

# Scale down
aws ecs update-service \
  --cluster healthapp-cluster \
  --service healthapp-service \
  --desired-count 1
``` 