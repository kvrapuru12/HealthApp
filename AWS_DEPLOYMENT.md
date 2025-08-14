# AWS Deployment Guide for HealthApp

## Overview

This guide explains how to deploy the HealthApp Spring Boot application to AWS using ECS Fargate, RDS MySQL, and Application Load Balancer.

## Architecture

```
Internet → ALB → ECS Fargate → RDS MySQL
                ↓
            ECR Repository
```

## Prerequisites

- AWS CLI configured
- Terraform installed
- Docker installed
- GitHub repository with CI/CD setup

## Infrastructure Setup

### 1. Using Terraform

The `terraform/` directory contains infrastructure as code:

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

### 2. Manual Setup

#### ECR Repository
```bash
aws ecr create-repository --repository-name healthapp
```

#### ECS Cluster
```bash
aws ecs create-cluster --cluster-name healthapp-cluster
```

#### RDS Database
```bash
aws rds create-db-instance \
  --db-instance-identifier healthapp-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --master-username admin \
  --master-user-password your-password
```

## Application Configuration

### Environment Variables

Set these in your ECS task definition:

```bash
DB_HOST=your-rds-endpoint
DB_USERNAME=admin
DB_PASSWORD=your-password
JWT_SECRET=your-jwt-secret
```

### Database Migration

The application uses Flyway for database migrations:

```bash
# Migrations run automatically on startup
# Located in src/main/resources/db/migration/
```

## Deployment Process

### 1. Build and Push Image

```bash
# Build Docker image
docker build -t healthapp .

# Tag for ECR
docker tag healthapp:latest your-account.dkr.ecr.region.amazonaws.com/healthapp:latest

# Push to ECR
docker push your-account.dkr.ecr.region.amazonaws.com/healthapp:latest
```

### 2. Deploy to ECS

```bash
# Update ECS service
aws ecs update-service \
  --cluster healthapp-cluster \
  --service healthapp-service \
  --force-new-deployment
```

## Monitoring and Logs

### CloudWatch Logs

ECS tasks automatically send logs to CloudWatch:

```bash
# View logs
aws logs tail /ecs/healthapp --follow
```

### Health Checks

The application exposes health endpoints:

- `/api/actuator/health` - Application health
- `/api/actuator/info` - Application info

## Scaling

### Auto Scaling

Configure ECS service auto-scaling:

```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/healthapp-cluster/healthapp-service \
  --min-capacity 1 \
  --max-capacity 10
```

## Security

### Network Security

- Use private subnets for ECS tasks Contra
- Use security groups to restrict access
- Enable VPC Flow Logs for monitoring

### IAM Roles

- ECS task execution role for ECR access
- ECS task role for application permissions
- Least privilege principle

## Troubleshooting

### Common Issues

1. **Database Connection**
   - Check security groups
   - Verify RDS endpoint
   - Check credentials

2. **Container Issues**
   - Check ECS task logs
   - Verify image exists in ECR
   - Check task definition

3. **Load Balancer**
   - Verify target group health
   - Check security groups
   - Verify listener rules

## Cost Optimization

- Use Spot instances for non-critical workloads
- Right-size RDS instance
- Enable auto-scaling
- Monitor CloudWatch metrics
