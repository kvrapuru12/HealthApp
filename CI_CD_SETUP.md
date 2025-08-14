# CI/CD Pipeline Setup for HealthApp

## Overview

This document describes the CI/CD pipeline setup for the HealthApp Spring Boot application using GitHub Actions and AWS services.

## Pipeline Components

### 1. GitHub Actions Workflows

The CI/CD pipeline consists of two main workflows:

- **`deploy.yml`** - Main deployment pipeline
- **`aws-deploy.yml`** - AWS-specific deployment

### 2. Pipeline Stages

1. **Test Stage**
   - Runs on every push and pull request
   - Executes unit tests
   - Builds the application

2. **Build & Deploy Stage**
   - Only runs on main branch pushes
   - Builds Docker image
   - Pushes to Amazon ECR
   - Deploys to ECS

## Required GitHub Secrets

Set these secrets in your GitHub repository:

```bash
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
```

## AWS Services Used

- **ECR** - Container registry
- **ECS** - Container orchestration
- **RDS** - Database
- **ALB** - Load balancer

## Local Development

For local development, you don't need the CI/CD setup:

```bash
# Run locally
mvn spring-boot:run

# Build
mvn clean package

# Test
mvn test
```

## Deployment

The pipeline automatically deploys when you push to the main branch:

```bash
git push origin main
```

## Monitoring

- Check GitHub Actions tab for pipeline status
- Monitor ECS service health
- Check application logs in CloudWatch
