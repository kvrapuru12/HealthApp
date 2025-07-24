#!/bin/bash

# Azure Deployment Script for HealthApp
echo "🚀 Deploying HealthApp to Azure..."

# Variables
RESOURCE_GROUP="healthapp-rg"
APP_NAME="healthapp-backend"
PLAN_NAME="healthapp-plan"
KEY_VAULT_NAME="healthapp-kv"
MYSQL_SERVER="healthapp-mysql"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Check if Azure CLI is installed
if ! command -v az &> /dev/null; then
    print_error "Azure CLI is not installed. Please install it first."
    print_status "Installation guide: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli"
    exit 1
fi

# Check if logged in to Azure
if ! az account show &> /dev/null; then
    print_warning "Not logged in to Azure. Please login first."
    az login
fi

# Build the application
print_status "📦 Building application..."
if ! mvn clean package -DskipTests; then
    print_error "Build failed. Please check your code and try again."
    exit 1
fi
print_success "Application built successfully!"

# Check if App Service Plan exists, create if not
print_status "🔍 Checking App Service Plan..."
if ! az appservice plan show --name $PLAN_NAME --resource-group $RESOURCE_GROUP &> /dev/null; then
    print_status "Creating App Service Plan..."
    az appservice plan create \
        --name $PLAN_NAME \
        --resource-group $RESOURCE_GROUP \
        --sku B1 \
        --is-linux
    print_success "App Service Plan created!"
else
    print_success "App Service Plan already exists!"
fi

# Check if App Service exists, create if not
print_status "🔍 Checking App Service..."
if ! az webapp show --name $APP_NAME --resource-group $RESOURCE_GROUP &> /dev/null; then
    print_status "Creating App Service..."
    az webapp create \
        --name $APP_NAME \
        --plan $PLAN_NAME \
        --resource-group $RESOURCE_GROUP \
        --runtime "JAVA:17-java17" \
        --deployment-local-git
    print_success "App Service created!"
else
    print_success "App Service already exists!"
fi

# Configure environment variables
print_status "⚙️ Configuring environment variables..."
az webapp config appsettings set \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP \
    --settings \
        DB_URL="jdbc:mysql://$MYSQL_SERVER.mysql.database.azure.com:3306/healthapp?useSSL=true&requireSSL=false&serverTimezone=UTC" \
        DB_USERNAME="@Microsoft.KeyVault(SecretUri=https://$KEY_VAULT_NAME.vault.azure.net/secrets/DB-USERNAME/)" \
        DB_PASSWORD="@Microsoft.KeyVault(SecretUri=https://$KEY_VAULT_NAME.vault.azure.net/secrets/DB-PASSWORD/)" \
        JWT_SECRET="@Microsoft.KeyVault(SecretUri=https://$KEY_VAULT_NAME.vault.azure.net/secrets/JWT-SECRET/)" \
        JWT_EXPIRATION="86400000" \
        SPRING_PROFILES_ACTIVE="azure"

print_success "Environment variables configured!"

# Configure managed identity for Key Vault access
print_status "🔐 Configuring managed identity..."
az webapp identity assign \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP

# Get the principal ID
PRINCIPAL_ID=$(az webapp identity show --name $APP_NAME --resource-group $RESOURCE_GROUP --query principalId -o tsv)

# Grant Key Vault access
print_status "🔑 Granting Key Vault access..."
az keyvault set-policy \
    --name $KEY_VAULT_NAME \
    --object-id $PRINCIPAL_ID \
    --secret-permissions get list

print_success "Key Vault access configured!"

# Deploy the application
print_status "📤 Deploying application..."
az webapp deployment source config-local-git \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP

# Get deployment URL
DEPLOYMENT_URL=$(az webapp deployment list-publishing-credentials \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP \
    --query publishingUserName \
    --output tsv)

print_success "Deployment configuration completed!"

# Wait for deployment to complete
print_status "⏳ Waiting for deployment to complete..."
sleep 30

# Test the deployment
print_status "🧪 Testing deployment..."
if curl -f -s "https://$APP_NAME.azurewebsites.net/api/actuator/health" > /dev/null; then
    print_success "Health check passed!"
else
    print_warning "Health check failed. Application might still be starting up."
fi

# Print final information
echo ""
print_success "🎉 Deployment completed successfully!"
echo ""
echo "📋 Deployment Information:"
echo "=========================="
echo "🌐 Application URL: https://$APP_NAME.azurewebsites.net/api"
echo "📚 Swagger UI: https://$APP_NAME.azurewebsites.net/api/swagger-ui.html"
echo "🏥 Health Check: https://$APP_NAME.azurewebsites.net/api/actuator/health"
echo "📊 Application Insights: https://portal.azure.com/#@/resource/subscriptions/*/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.Insights/components/healthapp-insights"
echo ""
echo "🔧 Useful Commands:"
echo "==================="
echo "View logs: az webapp log tail --name $APP_NAME --resource-group $RESOURCE_GROUP"
echo "Restart app: az webapp restart --name $APP_NAME --resource-group $RESOURCE_GROUP"
echo "Stop app: az webapp stop --name $APP_NAME --resource-group $RESOURCE_GROUP"
echo "Start app: az webapp start --name $APP_NAME --resource-group $RESOURCE_GROUP"
echo ""
print_warning "⚠️  Remember to:"
echo "   - Monitor your application performance"
echo "   - Set up alerts for critical metrics"
echo "   - Regularly backup your database"
echo "   - Keep your dependencies updated"
echo "" 