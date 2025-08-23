# Azure Deployment Guide for Opus Task Management System

This guide provides step-by-step instructions for deploying the Opus Task Management System to Microsoft Azure using Azure App Service.

## 📋 Prerequisites

Before starting the deployment, ensure you have:

- Azure account with an active subscription
- Azure CLI installed on your local machine
- Java 21 installed locally
- Maven installed locally
- Git repository with your Opus application code
- External PostgreSQL database (Neon) credentials

## 🚀 Deployment Options

We'll cover two deployment methods:

1. **Azure App Service** (Recommended for production)
2. **Azure Container Instances** (Alternative containerized approach)

---

## Method 1: Azure App Service Deployment

### Step 1: Install Azure CLI

Download and install Azure CLI from: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli

Verify installation:

```bash
az --version
```

### Step 2: Login to Azure

```bash
az login
```

This will open a browser window for authentication.

### Step 3: Create Resource Group

```bash
az group create --name opus-rg --location "East US"
```

### Step 4: Create App Service Plan

```bash
az appservice plan create \
  --name opus-app-plan \
  --resource-group opus-rg \
  --sku B1 \
  --is-linux
```

**SKU Options:**

- `F1` - Free tier (limited resources)
- `B1` - Basic tier (recommended for development)
- `S1` - Standard tier (recommended for production)
- `P1V2` - Premium tier (high performance)

### Step 5: Create Web App

```bash
az webapp create \
  --name opus-task-manager \
  --resource-group opus-rg \
  --plan opus-app-plan \
  --runtime "JAVA:21-java21"
```

**Note:** Replace `opus-task-manager` with your preferred unique app name.

### Step 6: Configure Application Settings

Set environment variables for your application:

```bash
# Database Configuration
az webapp config appsettings set \
  --name opus-task-manager \
  --resource-group opus-rg \
  --settings \
    SPRING_DATASOURCE_URL="jdbc:postgresql://ep-dawn-river-a1m8le9c-pooler.ap-southeast-1.aws.neon.tech/opus?user=neondb_owner&password=npg_NMZW5Vw0Gukp&sslmode=require&channelBinding=require" \
    SPRING_DATASOURCE_USERNAME="neondb_owner" \
    SPRING_DATASOURCE_PASSWORD="npg_NMZW5Vw0Gukp" \
    SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver"

# JPA Configuration
az webapp config appsettings set \
  --name opus-task-manager \
  --resource-group opus-rg \
  --settings \
    SPRING_JPA_HIBERNATE_DDL_AUTO="update" \
    SPRING_JPA_SHOW_SQL="false" \
    SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect"

# JWT Configuration
az webapp config appsettings set \
  --name opus-task-manager \
  --resource-group opus-rg \
  --settings \
    JWT_SECRET="OpusSecureJwtKey2025!@#$%^&*()_+{}|:<>?[]\\;',./-=`~1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" \
    JWT_EXPIRATION="86400000" \
    JWT_REFRESH_EXPIRATION="604800000"

# Email Configuration (Update with your credentials)
az webapp config appsettings set \
  --name opus-task-manager \
  --resource-group opus-rg \
  --settings \
    SPRING_MAIL_HOST="smtp.gmail.com" \
    SPRING_MAIL_PORT="587" \
    SPRING_MAIL_USERNAME="your-email@gmail.com" \
    SPRING_MAIL_PASSWORD="your-app-password"

# OAuth2 Configuration (Update with your credentials)
az webapp config appsettings set \
  --name opus-task-manager \
  --resource-group opus-rg \
  --settings \
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID="your-google-client-id" \
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET="your-google-client-secret"
```

### Step 7: Configure Redis (Optional - using Azure Cache for Redis)

Create Azure Cache for Redis:

```bash
az redis create \
  --name opus-redis \
  --resource-group opus-rg \
  --location "East US" \
  --sku Basic \
  --vm-size c0
```

Get Redis connection details:

```bash
az redis show --name opus-redis --resource-group opus-rg
az redis list-keys --name opus-redis --resource-group opus-rg
```

Configure Redis settings:

```bash
az webapp config appsettings set \
  --name opus-task-manager \
  --resource-group opus-rg \
  --settings \
    SPRING_DATA_REDIS_HOST="opus-redis.redis.cache.windows.net" \
    SPRING_DATA_REDIS_PORT="6380" \
    SPRING_DATA_REDIS_PASSWORD="your-redis-primary-key" \
    SPRING_DATA_REDIS_SSL="true"
```

### Step 8: Build and Deploy Application

#### Option A: Deploy from Local Machine

1. **Build the application:**

```bash
mvn clean package -DskipTests
```

2. **Deploy using Azure CLI:**

```bash
az webapp deploy \
  --name opus-task-manager \
  --resource-group opus-rg \
  --src-path target/opus-0.0.1-SNAPSHOT.jar \
  --type jar
```

#### Option B: Deploy from GitHub (Recommended)

1. **Configure GitHub deployment:**

```bash
az webapp deployment source config \
  --name opus-task-manager \
  --resource-group opus-rg \
  --repo-url https://github.com/Arkadipta-Kundu/opus \
  --branch main \
  --manual-integration
```

2. **Set up build configuration** by creating `.github/workflows/azure-deploy.yml`:

```yaml
name: Deploy to Azure App Service

on:
  push:
    branches: [main]
  workflow_dispatch:

env:
  AZURE_WEBAPP_NAME: opus-task-manager
  JAVA_VERSION: "21"

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: "microsoft"

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Deploy to Azure Web App
        uses: azure/webapps-deploy@v2
        with:
          app-name: ${{ env.AZURE_WEBAPP_NAME }}
          publish-profile: ${{ secrets.AZURE_WEBAPP_PUBLISH_PROFILE }}
          package: "./target/opus-0.0.1-SNAPSHOT.jar"
```

3. **Configure GitHub secrets:**
   - Go to your GitHub repository → Settings → Secrets
   - Add `AZURE_WEBAPP_PUBLISH_PROFILE` secret with the publish profile from Azure

Get publish profile:

```bash
az webapp deployment list-publishing-profiles \
  --name opus-task-manager \
  --resource-group opus-rg \
  --xml
```

### Step 9: Configure Custom Domain (Optional)

```bash
# Add custom domain
az webapp config hostname add \
  --webapp-name opus-task-manager \
  --resource-group opus-rg \
  --hostname yourdomain.com

# Configure SSL certificate
az webapp config ssl bind \
  --certificate-thumbprint <thumbprint> \
  --ssl-type SNI \
  --name opus-task-manager \
  --resource-group opus-rg
```

### Step 10: Configure Application Insights (Monitoring)

```bash
# Create Application Insights
az monitor app-insights component create \
  --app opus-insights \
  --location "East US" \
  --resource-group opus-rg

# Get instrumentation key
az monitor app-insights component show \
  --app opus-insights \
  --resource-group opus-rg

# Configure App Service to use Application Insights
az webapp config appsettings set \
  --name opus-task-manager \
  --resource-group opus-rg \
  --settings \
    APPLICATIONINSIGHTS_CONNECTION_STRING="your-connection-string"
```

---

## Method 2: Azure Container Instances Deployment

### Step 1: Create Dockerfile

Create `Dockerfile` in your project root:

```dockerfile
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the jar file
COPY target/opus-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 2: Create Azure Container Registry

```bash
# Create ACR
az acr create \
  --name opusregistry \
  --resource-group opus-rg \
  --sku Basic \
  --admin-enabled true

# Login to ACR
az acr login --name opusregistry
```

### Step 3: Build and Push Docker Image

```bash
# Build the application
mvn clean package -DskipTests

# Build Docker image
docker build -t opusregistry.azurecr.io/opus:latest .

# Push to ACR
docker push opusregistry.azurecr.io/opus:latest
```

### Step 4: Deploy to Azure Container Instances

```bash
# Get ACR credentials
az acr credential show --name opusregistry

# Create container instance
az container create \
  --name opus-container \
  --resource-group opus-rg \
  --image opusregistry.azurecr.io/opus:latest \
  --cpu 1 \
  --memory 2 \
  --ports 8080 \
  --dns-name-label opus-task-manager \
  --registry-login-server opusregistry.azurecr.io \
  --registry-username opusregistry \
  --registry-password <acr-password> \
  --environment-variables \
    SPRING_DATASOURCE_URL="jdbc:postgresql://ep-dawn-river-a1m8le9c-pooler.ap-southeast-1.aws.neon.tech/opus?user=neondb_owner&password=npg_NMZW5Vw0Gukp&sslmode=require&channelBinding=require" \
    SPRING_DATASOURCE_USERNAME="neondb_owner" \
    SPRING_DATASOURCE_PASSWORD="npg_NMZW5Vw0Gukp"
```

---

## 🔧 Post-Deployment Configuration

### 1. Update OAuth2 Redirect URI

Update your Google OAuth2 configuration to include the Azure URL:

```
https://opus-task-manager.azurewebsites.net/login/oauth2/code/google
```

### 2. Configure CORS (if needed)

```bash
az webapp cors add \
  --name opus-task-manager \
  --resource-group opus-rg \
  --allowed-origins "https://yourdomain.com"
```

### 3. Enable HTTPS Only

```bash
az webapp update \
  --name opus-task-manager \
  --resource-group opus-rg \
  --https-only true
```

### 4. Configure Health Check

```bash
az webapp config set \
  --name opus-task-manager \
  --resource-group opus-rg \
  --health-check-path "/public/health"
```

---

## 🔍 Monitoring and Troubleshooting

### View Application Logs

```bash
# Stream live logs
az webapp log tail \
  --name opus-task-manager \
  --resource-group opus-rg

# Download logs
az webapp log download \
  --name opus-task-manager \
  --resource-group opus-rg
```

### Check Application Status

```bash
# Get app details
az webapp show \
  --name opus-task-manager \
  --resource-group opus-rg

# Check health
curl https://opus-task-manager.azurewebsites.net/public/health
```

### Common Issues and Solutions

1. **Application Won't Start:**

   - Check logs for error messages
   - Verify environment variables are set correctly
   - Ensure database connectivity

2. **Database Connection Issues:**

   - Verify connection string format
   - Check firewall rules
   - Ensure SSL settings are correct

3. **Memory Issues:**

   - Upgrade to higher SKU (S1, P1V2)
   - Optimize JVM settings

4. **Performance Issues:**
   - Enable Application Insights
   - Monitor resource usage
   - Consider scaling up or out

---

## 🔄 Scaling and Production Considerations

### Auto Scaling

```bash
# Create autoscale profile
az monitor autoscale create \
  --resource-group opus-rg \
  --resource opus-task-manager \
  --resource-type Microsoft.Web/serverfarms \
  --name opus-autoscale \
  --min-count 1 \
  --max-count 3 \
  --count 1

# Add CPU-based scaling rule
az monitor autoscale rule create \
  --resource-group opus-rg \
  --autoscale-name opus-autoscale \
  --condition "Percentage CPU > 70 avg 5m" \
  --scale out 1
```

### Backup Configuration

```bash
# Enable backup
az webapp config backup create \
  --webapp-name opus-task-manager \
  --resource-group opus-rg \
  --backup-name opus-backup \
  --storage-account-url "your-storage-url"
```

### Production Checklist

- [ ] Use Standard or Premium App Service plan
- [ ] Configure custom domain with SSL
- [ ] Enable Application Insights monitoring
- [ ] Set up automated backups
- [ ] Configure auto-scaling
- [ ] Use Azure Key Vault for secrets
- [ ] Set up staging slots for zero-downtime deployments
- [ ] Configure Azure Front Door or CDN
- [ ] Set up alerts and monitoring

---

## 🔐 Security Best Practices

1. **Use Azure Key Vault for Secrets:**

```bash
# Create Key Vault
az keyvault create \
  --name opus-keyvault \
  --resource-group opus-rg \
  --location "East US"

# Store secrets
az keyvault secret set \
  --vault-name opus-keyvault \
  --name "database-password" \
  --value "your-password"
```

2. **Enable Managed Identity:**

```bash
az webapp identity assign \
  --name opus-task-manager \
  --resource-group opus-rg
```

3. **Configure Network Security:**
   - Use Azure Application Gateway with WAF
   - Configure VNet integration
   - Implement IP restrictions if needed

---

## 💰 Cost Optimization

1. **Choose appropriate SKU:**

   - Development: B1 or F1 (free)
   - Production: S1 or P1V2

2. **Use reserved instances** for production workloads

3. **Implement auto-scaling** to handle traffic spikes

4. **Monitor costs** using Azure Cost Management

---

## 📚 Useful Commands

### Quick Deployment Script

Create `deploy-to-azure.sh`:

```bash
#!/bin/bash

# Variables
RESOURCE_GROUP="opus-rg"
APP_NAME="opus-task-manager"
LOCATION="East US"

# Build application
echo "Building application..."
mvn clean package -DskipTests

# Deploy to Azure
echo "Deploying to Azure..."
az webapp deploy \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --src-path target/opus-0.0.1-SNAPSHOT.jar \
  --type jar

echo "Deployment complete!"
echo "App URL: https://$APP_NAME.azurewebsites.net"
```

### Environment Management

```bash
# Create staging slot
az webapp deployment slot create \
  --name opus-task-manager \
  --resource-group opus-rg \
  --slot staging

# Swap slots (zero-downtime deployment)
az webapp deployment slot swap \
  --name opus-task-manager \
  --resource-group opus-rg \
  --slot staging \
  --target-slot production
```

---

## 🎯 Final Steps

1. **Test the deployment:**

   - Access your app at `https://opus-task-manager.azurewebsites.net`
   - Test API endpoints using the provided documentation
   - Verify database connectivity and operations

2. **Set up monitoring:**

   - Configure Application Insights dashboards
   - Set up alerts for critical metrics
   - Monitor application performance

3. **Configure CI/CD:**
   - Set up GitHub Actions workflow
   - Configure automated testing
   - Implement deployment gates

Your Opus Task Management System is now deployed on Azure! 🚀

For support and updates, refer to:

- [Azure App Service Documentation](https://docs.microsoft.com/en-us/azure/app-service/)
- [Spring Boot on Azure Guide](https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/)
- [Azure CLI Reference](https://docs.microsoft.com/en-us/cli/azure/)
