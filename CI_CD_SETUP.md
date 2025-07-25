# CI/CD Pipeline Setup Guide

Complete guide to set up automated deployment to AWS using GitHub Actions.

## 🎯 Overview

This setup provides:
- ✅ **Automated testing** on every push/PR
- ✅ **Automated deployment** to AWS on main branch
- ✅ **Infrastructure as Code** with Terraform
- ✅ **Container deployment** with ECS Fargate
- ✅ **Database management** with RDS MySQL
- ✅ **Load balancing** with ALB
- ✅ **Monitoring** with CloudWatch

## 📋 Prerequisites

1. **AWS Account** with appropriate permissions
2. **GitHub Repository** with your HealthApp code
3. **AWS CLI** installed and configured locally
4. **Terraform** installed (optional, for infrastructure setup)

## 🔧 Step 1: AWS Infrastructure Setup

### Option A: Using Terraform (Recommended)

```bash
# Navigate to terraform directory
cd terraform

# Initialize Terraform
terraform init

# Create terraform.tfvars file
cat > terraform.tfvars << EOF
aws_region = "us-east-1"
db_password = "YourSecurePassword123!"
jwt_secret = "$(openssl rand -hex 32)"
environment = "production"
EOF

# Plan the infrastructure
terraform plan

# Apply the infrastructure
terraform apply
```

### Option B: Manual Setup

Follow the instructions in `AWS_DEPLOYMENT.md` to manually create the AWS infrastructure.

## 🔐 Step 2: GitHub Secrets Configuration

Go to your GitHub repository → Settings → Secrets and variables → Actions, and add the following secrets:

### Required Secrets:
- `AWS_ACCESS_KEY_ID` - Your AWS access key
- `AWS_SECRET_ACCESS_KEY` - Your AWS secret key

### Optional Secrets (if using custom values):
- `AWS_REGION` - AWS region (default: us-east-1)
- `ECR_REPOSITORY` - ECR repository name (default: healthapp)
- `ECS_CLUSTER` - ECS cluster name (default: healthapp-cluster)
- `ECS_SERVICE` - ECS service name (default: healthapp-service)

## 🚀 Step 3: GitHub Actions Workflow

The workflow file `.github/workflows/deploy.yml` is already configured and will:

1. **Test Job** (runs on all pushes/PRs):
   - Checkout code
   - Set up Java 17
   - Run tests
   - Build application

2. **Deploy Job** (runs only on main branch):
   - Build Docker image
   - Push to ECR
   - Deploy to ECS
   - Test deployment

## 🔄 Step 4: Trigger Deployment

### Automatic Deployment
Simply push to the main branch:
```bash
git add .
git commit -m "Update application"
git push origin main
```

### Manual Deployment
You can also trigger the workflow manually:
1. Go to GitHub repository → Actions
2. Select "Deploy HealthApp to AWS"
3. Click "Run workflow"

## 📊 Step 5: Monitoring and Verification

### Check Deployment Status
1. **GitHub Actions**: Go to Actions tab to see deployment progress
2. **AWS ECS Console**: Monitor service status
3. **CloudWatch Logs**: View application logs

### Test Application
After deployment, test the endpoints:
```bash
# Get ALB DNS name from Terraform outputs or AWS console
ALB_DNS="your-alb-dns-name.us-east-1.elb.amazonaws.com"

# Test health endpoint
curl https://$ALB_DNS/api/actuator/health

# Test Swagger UI
curl https://$ALB_DNS/api/swagger-ui.html
```

## 🔧 Step 6: Environment Configuration

### Database Connection
The application automatically connects to RDS using environment variables:
- `DB_HOST` - RDS endpoint (automatically set by ECS)
- `DB_USERNAME` - Database username (admin)
- `SPRING_DATASOURCE_PASSWORD` - Database password (from Secrets Manager)

### JWT Configuration
- `JWT_SECRET` - JWT secret key (from Secrets Manager)

## 🛠️ Step 7: Customization

### Update Application Properties
Modify `src/main/resources/application-aws.properties` for AWS-specific settings.

### Update Infrastructure
Modify Terraform files in the `terraform/` directory to customize:
- VPC configuration
- Security groups
- ECS task definition
- Load balancer settings

### Update Workflow
Modify `.github/workflows/deploy.yml` to:
- Add additional test steps
- Change deployment strategy
- Add notifications
- Customize build process

## 🔍 Troubleshooting

### Common Issues

1. **Build Failures**
   - Check Java version compatibility
   - Verify Maven dependencies
   - Review test failures

2. **Deployment Failures**
   - Verify AWS credentials
   - Check ECR repository exists
   - Ensure ECS cluster and service exist
   - Review CloudWatch logs

3. **Application Issues**
   - Check database connectivity
   - Verify environment variables
   - Review application logs

### Debug Commands

```bash
# Check ECS service status
aws ecs describe-services --cluster healthapp-cluster --services healthapp-service

# View CloudWatch logs
aws logs describe-log-streams --log-group-name /ecs/healthapp

# Test database connectivity
aws rds describe-db-instances --db-instance-identifier healthapp-db
```

## 📈 Step 8: Scaling and Maintenance

### Scale Application
```bash
# Scale up
aws ecs update-service --cluster healthapp-cluster --service healthapp-service --desired-count 4

# Scale down
aws ecs update-service --cluster healthapp-cluster --service healthapp-service --desired-count 1
```

### Update Application
Simply push to main branch - the CI/CD pipeline will handle the rest!

### Infrastructure Updates
```bash
cd terraform
terraform plan
terraform apply
```

## 🎉 Success!

Your HealthApp now has:
- ✅ **Automated CI/CD pipeline**
- ✅ **Production-ready AWS infrastructure**
- ✅ **Scalable container deployment**
- ✅ **Managed database**
- ✅ **Load balancing**
- ✅ **Monitoring and logging**

## 📞 Support

For issues or questions:
1. Check GitHub Actions logs
2. Review CloudWatch logs
3. Verify AWS infrastructure status
4. Test application endpoints

---

**Next Steps:**
- Set up custom domain with Route 53
- Configure SSL certificates
- Set up monitoring alerts
- Implement blue-green deployments 