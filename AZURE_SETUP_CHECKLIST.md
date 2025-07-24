# Azure Deployment Checklist

Complete checklist for deploying HealthApp to Azure.

## ✅ Prerequisites

- [ ] **Azure CLI** installed and configured
- [ ] **Java 17+** installed
- [ ] **Maven 3.6+** installed
- [ ] **Azure Account** with active subscription

## 🔧 Azure Infrastructure Setup

### 1. Resource Group
- [ ] Create resource group
  ```bash
  az group create --name healthapp-rg --location "East US"
  az configure --defaults group=healthapp-rg
  ```

### 2. Azure Database for MySQL
- [ ] Create MySQL server
  ```bash
  az mysql flexible-server create \
    --name healthapp-mysql \
    --admin-user healthappadmin \
    --admin-password "YourSecurePassword123!" \
    --sku-name Standard_B1ms \
    --tier Burstable \
    --storage-size 20 \
    --version 8.0.21
  ```
- [ ] Create database
  ```bash
  az mysql flexible-server db create \
    --server-name healthapp-mysql \
    --database-name healthapp
  ```
- [ ] Configure firewall rules
  ```bash
  az mysql flexible-server firewall-rule create \
    --name AllowAzureServices \
    --start-ip-address 0.0.0.0 \
    --end-ip-address 0.0.0.0
  ```

### 3. Azure Key Vault
- [ ] Create Key Vault
  ```bash
  az keyvault create \
    --name healthapp-kv \
    --resource-group healthapp-rg \
    --location "East US"
  ```
- [ ] Store secrets
  ```bash
  az keyvault secret set --vault-name healthapp-kv --name "DB-USERNAME" --value "healthappadmin"
  az keyvault secret set --vault-name healthapp-kv --name "DB-PASSWORD" --value "YourSecurePassword123!"
  az keyvault secret set --vault-name healthapp-kv --name "JWT-SECRET" --value "$(openssl rand -hex 32)"
  ```

### 4. Azure App Service Plan
- [ ] Create App Service Plan
  ```bash
  az appservice plan create --name healthapp-plan --sku B1 --is-linux
  ```

### 5. Azure Application Insights
- [ ] Create Application Insights
  ```bash
  az monitor app-insights component create \
    --app healthapp-insights \
    --location "East US" \
    --kind web
  ```

## 🚀 Deployment

### Option 1: Automated Deployment
- [ ] Run deployment script
  ```bash
  ./deploy-azure.sh
  ```

### Option 2: Manual Deployment
- [ ] Build application
  ```bash
  mvn clean package -DskipTests
  ```
- [ ] Create App Service
  ```bash
  az webapp create \
    --name healthapp-backend \
    --plan healthapp-plan \
    --resource-group healthapp-rg \
    --runtime "JAVA:17-java17"
  ```
- [ ] Configure environment variables
- [ ] Deploy application

### Option 3: CI/CD (GitHub Actions)
- [ ] Set up Azure credentials in GitHub Secrets
- [ ] Push to main branch to trigger deployment

## 🧪 Verification

- [ ] Health check: `https://healthapp-backend.azurewebsites.net/api/actuator/health`
- [ ] Swagger UI: `https://healthapp-backend.azurewebsites.net/api/swagger-ui.html`
- [ ] API endpoints: `https://healthapp-backend.azurewebsites.net/api/users`

## 🔐 Security

- [ ] Verify Key Vault access
- [ ] Confirm database SSL is enabled
- [ ] Test JWT authentication
- [ ] Verify environment variables are secure

## 📊 Monitoring

- [ ] Verify Application Insights is collecting data
- [ ] Set up alerts for critical metrics
- [ ] Configure log analytics

## 💰 Cost Management

- [ ] Set up cost alerts
- [ ] Monitor resource usage
- [ ] Optimize resource sizing

---

## 🆘 Troubleshooting

### Common Issues

**Database Connection Issues**
```bash
az mysql flexible-server firewall-rule list --name healthapp-mysql
```

**Key Vault Access Issues**
```bash
az keyvault show --name healthapp-kv --query properties.accessPolicies
```

**Application Startup Issues**
```bash
az webapp log tail --name healthapp-backend --resource-group healthapp-rg
```

---

**🎉 Success!** Your HealthApp is now deployed to Azure with proper security, monitoring, and scalability. 