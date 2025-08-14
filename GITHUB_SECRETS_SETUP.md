# GitHub Secrets Setup for HealthApp CI/CD

## Overview

This document explains how to set up the required GitHub secrets for the CI/CD pipeline to work properly.

## Required Secrets

### 1. AWS Credentials

These are essential for deploying to AWS:

#### AWS_ACCESS_KEY_ID
- **Purpose**: AWS access key for authentication
- **Format**: AKIA... (20 characters)
- **Permissions**: Should have ECR, ECS, and related permissions

#### AWS_SECRET_ACCESS_KEY
- **Purpose**: AWS secret key for authentication
- **Format**: Secret string (40 characters)
- **Security**: Keep this highly secure

## How to Set Up Secrets

### Step 1: Create AWS IAM User

1. Go to AWS IAM Console
2. Create a new user with programmatic access
3. Attach policies for:
   - ECR (Elastic Container Registry)
   - ECS (Elastic Container Service)
   - S3 (if needed for artifacts)

### Step 2: Add Secrets to GitHub

1. Go to your GitHub repository
2. Click **Settings** tab
3. Click **Secrets and variables** → **Actions**
4. Click **New repository secret**
5. Add each secret:
   - Name: `AWS_ACCESS_KEY_ID`
   - Value: Your AWS access key
   - Repeat for `AWS_SECRET_ACCESS_KEY`

## IAM Policy Example

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
                "ecs:UpdateService"
            ],
            "Resource": "*"
        }
    ]
}
```

## Testing the Setup

After setting up secrets:

1. Push a change to main branch
2. Check GitHub Actions tab
3. Verify the pipeline runs successfully
4. Check AWS console for deployed resources

## Security Best Practices

- Use least privilege principle for IAM policies
- Rotate access keys regularly
- Monitor AWS CloudTrail for access logs
- Never commit secrets to code
- Use AWS Secrets Manager for production
