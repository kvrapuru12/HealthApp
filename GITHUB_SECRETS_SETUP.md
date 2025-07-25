# GitHub Secrets Setup Guide

This guide explains how to set up the required GitHub secrets for the automated AWS deployment pipeline.

## Required Secrets

You need to configure the following secrets in your GitHub repository:

### 1. AWS_ACCESS_KEY_ID
- **Description**: AWS Access Key ID for programmatic access
- **How to get it**:
  1. Go to AWS IAM Console
  2. Create a new user or use existing user
  3. Attach the required policies (see below)
  4. Create access keys
  5. Copy the Access Key ID

### 2. AWS_SECRET_ACCESS_KEY
- **Description**: AWS Secret Access Key for programmatic access
- **How to get it**:
  1. Same as above - when creating access keys
  2. Copy the Secret Access Key (only shown once)

## IAM User Setup

Create an IAM user with the following policies attached:

### Required Policies:
1. **AmazonEC2ContainerRegistryPowerUser** - For ECR access
2. **AmazonECS-FullAccess** - For ECS deployment
3. **AmazonElasticLoadBalancingFullAccess** - For ALB operations
4. **SecretsManagerReadWrite** - For accessing secrets

### Custom Policy (Alternative):
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
                "ecs:DescribeServices",
                "ecs:DescribeClusters"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "elasticloadbalancing:DescribeLoadBalancers"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "secretsmanager:GetSecretValue"
            ],
            "Resource": "arn:aws:secretsmanager:us-east-1:*:secret:healthapp/*"
        }
    ]
}
```

## How to Add Secrets to GitHub

1. **Go to your GitHub repository**
2. **Click on "Settings" tab**
3. **Click on "Secrets and variables" → "Actions"**
4. **Click "New repository secret"**
5. **Add each secret**:
   - Name: `AWS_ACCESS_KEY_ID`
   - Value: Your AWS Access Key ID
   - Name: `AWS_SECRET_ACCESS_KEY`
   - Value: Your AWS Secret Access Key

## Verification

After setting up the secrets:

1. **Push a change to main branch**
2. **Go to "Actions" tab in GitHub**
3. **Check if the workflow runs successfully**
4. **Verify deployment in AWS ECS console**

## Security Best Practices

- ✅ Use IAM users with minimal required permissions
- ✅ Rotate access keys regularly
- ✅ Use AWS Organizations for better access control
- ✅ Enable CloudTrail for audit logging
- ✅ Use AWS Secrets Manager for sensitive data

## Troubleshooting

### Common Issues:

1. **"Access Denied" errors**:
   - Check IAM user permissions
   - Verify access keys are correct
   - Ensure resources exist in the specified region

2. **ECR login failures**:
   - Verify ECR repository exists
   - Check ECR permissions

3. **ECS deployment failures**:
   - Check ECS cluster and service exist
   - Verify task definition is valid
   - Check security groups and networking

### Debug Steps:

1. Check GitHub Actions logs for detailed error messages
2. Verify AWS resources exist in the correct region
3. Test AWS CLI commands locally with the same credentials
4. Check CloudWatch logs for application errors 