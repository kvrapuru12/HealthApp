#!/bin/bash

# HealthApp CI/CD Setup Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
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

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
    
    # Check if AWS credentials are configured
    if ! aws sts get-caller-identity &> /dev/null; then
        print_error "AWS credentials not configured. Please run 'aws configure' first."
        exit 1
    fi
    
    # Check if git is available
    if ! command -v git &> /dev/null; then
        print_error "Git is not installed. Please install it first."
        exit 1
    fi
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository. Please initialize git first."
        exit 1
    fi
    
    print_success "All prerequisites met!"
}

# Get AWS account ID
get_aws_account_id() {
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    print_status "AWS Account ID: $AWS_ACCOUNT_ID"
}

# Setup Terraform
setup_terraform() {
    print_status "Setting up Terraform infrastructure..."
    
    if [ ! -d "terraform" ]; then
        print_error "Terraform directory not found. Please ensure terraform files are present."
        exit 1
    fi
    
    cd terraform
    
    # Initialize Terraform
    print_status "Initializing Terraform..."
    terraform init
    
    # Create terraform.tfvars if it doesn't exist
    if [ ! -f "terraform.tfvars" ]; then
        print_status "Creating terraform.tfvars..."
        cat > terraform.tfvars << EOF
aws_region = "us-east-1"
db_password = "$(openssl rand -base64 32)"
jwt_secret = "$(openssl rand -hex 32)"
environment = "production"
EOF
        print_success "Created terraform.tfvars with secure random values"
    else
        print_warning "terraform.tfvars already exists. Skipping creation."
    fi
    
    # Plan Terraform
    print_status "Planning Terraform infrastructure..."
    terraform plan
    
    print_warning "Review the plan above. To apply, run: terraform apply"
    
    cd ..
}

# Setup GitHub repository
setup_github() {
    print_status "Setting up GitHub repository..."
    
    # Get repository URL
    REPO_URL=$(git remote get-url origin)
    if [ -z "$REPO_URL" ]; then
        print_error "No remote origin found. Please add your GitHub repository as origin."
        exit 1
    fi
    
    print_success "Repository URL: $REPO_URL"
    
    # Check if GitHub Actions directory exists
    if [ ! -d ".github/workflows" ]; then
        print_error "GitHub Actions workflow not found. Please ensure .github/workflows/deploy.yml exists."
        exit 1
    fi
    
    print_success "GitHub Actions workflow found"
}

# Generate secrets template
generate_secrets_template() {
    print_status "Generating GitHub secrets template..."
    
    cat > github-secrets-template.md << EOF
# GitHub Secrets Configuration

Add the following secrets to your GitHub repository:

## Required Secrets

Go to your repository → Settings → Secrets and variables → Actions

### AWS Credentials
- **Name**: \`AWS_ACCESS_KEY_ID\`
- **Value**: Your AWS access key ID

- **Name**: \`AWS_SECRET_ACCESS_KEY\`
- **Value**: Your AWS secret access key

## Optional Secrets (if using custom values)

- **Name**: \`AWS_REGION\`
- **Value**: \`us-east-1\` (default)

- **Name**: \`ECR_REPOSITORY\`
- **Value**: \`healthapp\` (default)

- **Name**: \`ECS_CLUSTER\`
- **Value**: \`healthapp-cluster\` (default)

- **Name**: \`ECS_SERVICE\`
- **Value**: \`healthapp-service\` (default)

## How to add secrets:

1. Go to your GitHub repository
2. Click on "Settings" tab
3. Click on "Secrets and variables" → "Actions"
4. Click "New repository secret"
5. Add each secret above

## AWS IAM Permissions Required

Your AWS user/role needs the following permissions:
- ECR: Full access
- ECS: Full access
- IAM: Read access (for task execution role)
- Secrets Manager: Read access
- CloudWatch Logs: Write access
EOF
    
    print_success "Generated github-secrets-template.md"
}

# Main setup function
main() {
    print_status "Starting HealthApp CI/CD setup..."
    
    check_prerequisites
    get_aws_account_id
    setup_terraform
    setup_github
    generate_secrets_template
    
    print_success "🎉 Setup completed!"
    echo ""
    echo "📋 Next steps:"
    echo "1. Review and apply Terraform infrastructure:"
    echo "   cd terraform && terraform apply"
    echo ""
    echo "2. Configure GitHub secrets (see github-secrets-template.md)"
    echo ""
    echo "3. Push your code to trigger the first deployment:"
    echo "   git add . && git commit -m 'Setup CI/CD pipeline' && git push origin main"
    echo ""
    echo "📚 Documentation:"
    echo "- CI/CD Setup Guide: CI_CD_SETUP.md"
    echo "- AWS Deployment Guide: AWS_DEPLOYMENT.md"
    echo "- GitHub Secrets Template: github-secrets-template.md"
}

# Run main function
main "$@" 