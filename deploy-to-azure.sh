#!/bin/bash

# Opus Azure Deployment Script
# This script automates the deployment of Opus Task Management System to Azure

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
RESOURCE_GROUP="opus-rg"
APP_NAME="opus-task-manager"
LOCATION="East US"
APP_SERVICE_PLAN="opus-app-plan"
SKU="B1"

echo -e "${BLUE}🚀 Opus Azure Deployment Script${NC}"
echo "=================================="

# Function to print status
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if Azure CLI is installed
if ! command -v az &> /dev/null; then
    print_error "Azure CLI is not installed. Please install it first."
    exit 1
fi

# Check if user is logged in
if ! az account show &> /dev/null; then
    print_warning "You are not logged in to Azure. Please log in..."
    az login
fi

print_status "Azure CLI is ready!"

# Get unique app name
read -p "Enter a unique app name (or press Enter for default 'opus-task-manager'): " user_app_name
if [ ! -z "$user_app_name" ]; then
    APP_NAME="$user_app_name"
fi

echo -e "${BLUE}Deploying with app name: $APP_NAME${NC}"

# Step 1: Create Resource Group
echo -e "\n${BLUE}Step 1: Creating Resource Group${NC}"
if az group show --name $RESOURCE_GROUP &> /dev/null; then
    print_warning "Resource group '$RESOURCE_GROUP' already exists"
else
    az group create --name $RESOURCE_GROUP --location "$LOCATION"
    print_status "Resource group created"
fi

# Step 2: Create App Service Plan
echo -e "\n${BLUE}Step 2: Creating App Service Plan${NC}"
if az appservice plan show --name $APP_SERVICE_PLAN --resource-group $RESOURCE_GROUP &> /dev/null; then
    print_warning "App Service plan '$APP_SERVICE_PLAN' already exists"
else
    az appservice plan create \
        --name $APP_SERVICE_PLAN \
        --resource-group $RESOURCE_GROUP \
        --sku $SKU \
        --is-linux
    print_status "App Service plan created"
fi

# Step 3: Create Web App
echo -e "\n${BLUE}Step 3: Creating Web App${NC}"
if az webapp show --name $APP_NAME --resource-group $RESOURCE_GROUP &> /dev/null; then
    print_warning "Web app '$APP_NAME' already exists"
else
    az webapp create \
        --name $APP_NAME \
        --resource-group $RESOURCE_GROUP \
        --plan $APP_SERVICE_PLAN \
        --runtime "JAVA:21-java21"
    print_status "Web app created"
fi

# Step 4: Configure Application Settings
echo -e "\n${BLUE}Step 4: Configuring Application Settings${NC}"

# Database Configuration
az webapp config appsettings set \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP \
    --settings \
        SPRING_DATASOURCE_URL="jdbc:postgresql://ep-dawn-river-a1m8le9c-pooler.ap-southeast-1.aws.neon.tech/opus?user=neondb_owner&password=npg_NMZW5Vw0Gukp&sslmode=require&channelBinding=require" \
        SPRING_DATASOURCE_USERNAME="neondb_owner" \
        SPRING_DATASOURCE_PASSWORD="npg_NMZW5Vw0Gukp" \
        SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver" \
        > /dev/null

print_status "Database configuration set"

# JPA Configuration
az webapp config appsettings set \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP \
    --settings \
        SPRING_JPA_HIBERNATE_DDL_AUTO="update" \
        SPRING_JPA_SHOW_SQL="false" \
        SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect" \
        > /dev/null

print_status "JPA configuration set"

# JWT Configuration
az webapp config appsettings set \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP \
    --settings \
        JWT_SECRET="OpusSecureJwtKey2025!@#$%^&*()_+{}|:<>?[]\\;',./-=`~1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" \
        JWT_EXPIRATION="86400000" \
        JWT_REFRESH_EXPIRATION="604800000" \
        > /dev/null

print_status "JWT configuration set"

# Optional: Email Configuration
echo -e "\n${YELLOW}Email Configuration (Optional)${NC}"
read -p "Do you want to configure email settings? (y/n): " configure_email

if [ "$configure_email" = "y" ] || [ "$configure_email" = "Y" ]; then
    read -p "Enter Gmail username: " gmail_username
    read -s -p "Enter Gmail app password: " gmail_password
    echo
    
    az webapp config appsettings set \
        --name $APP_NAME \
        --resource-group $RESOURCE_GROUP \
        --settings \
            SPRING_MAIL_HOST="smtp.gmail.com" \
            SPRING_MAIL_PORT="587" \
            SPRING_MAIL_USERNAME="$gmail_username" \
            SPRING_MAIL_PASSWORD="$gmail_password" \
            SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true" \
            SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE="true" \
            > /dev/null
    
    print_status "Email configuration set"
fi

# Step 5: Build Application
echo -e "\n${BLUE}Step 5: Building Application${NC}"
if [ -f "pom.xml" ]; then
    mvn clean package -DskipTests
    print_status "Application built successfully"
else
    print_error "pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

# Step 6: Deploy Application
echo -e "\n${BLUE}Step 6: Deploying Application${NC}"
az webapp deploy \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP \
    --src-path target/opus-0.0.1-SNAPSHOT.jar \
    --type jar

print_status "Application deployed successfully!"

# Step 7: Configure Security Settings
echo -e "\n${BLUE}Step 7: Configuring Security${NC}"

# Enable HTTPS only
az webapp update \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP \
    --https-only true > /dev/null

print_status "HTTPS-only enabled"

# Configure health check
az webapp config set \
    --name $APP_NAME \
    --resource-group $RESOURCE_GROUP \
    --health-check-path "/public/health" > /dev/null

print_status "Health check configured"

# Final Information
echo -e "\n${GREEN}🎉 Deployment Completed Successfully!${NC}"
echo "======================================"
echo -e "${BLUE}Application URL:${NC} https://$APP_NAME.azurewebsites.net"
echo -e "${BLUE}Health Check:${NC} https://$APP_NAME.azurewebsites.net/public/health"
echo -e "${BLUE}API Documentation:${NC} https://$APP_NAME.azurewebsites.net/swagger-ui.html"
echo -e "${BLUE}Resource Group:${NC} $RESOURCE_GROUP"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Test your application at the URL above"
echo "2. Configure OAuth2 redirect URI in Google Console:"
echo "   https://$APP_NAME.azurewebsites.net/login/oauth2/code/google"
echo "3. Set up monitoring and alerts"
echo "4. Configure custom domain (optional)"
echo ""
echo -e "${BLUE}To view logs:${NC}"
echo "az webapp log tail --name $APP_NAME --resource-group $RESOURCE_GROUP"
echo ""
echo -e "${BLUE}To update the application:${NC}"
echo "1. mvn clean package -DskipTests"
echo "2. az webapp deploy --name $APP_NAME --resource-group $RESOURCE_GROUP --src-path target/opus-0.0.1-SNAPSHOT.jar --type jar"

# Optional: Open browser
read -p "Do you want to open the application in your browser? (y/n): " open_browser
if [ "$open_browser" = "y" ] || [ "$open_browser" = "Y" ]; then
    if command -v xdg-open &> /dev/null; then
        xdg-open "https://$APP_NAME.azurewebsites.net"
    elif command -v open &> /dev/null; then
        open "https://$APP_NAME.azurewebsites.net"
    else
        echo "Please open https://$APP_NAME.azurewebsites.net in your browser"
    fi
fi

echo -e "\n${GREEN}Deployment script completed!${NC}"
