@echo off
setlocal enabledelayedexpansion

REM Opus Azure Deployment Script for Windows
REM This script automates the deployment of Opus Task Management System to Azure

echo.
echo ==========================================
echo    Opus Azure Deployment Script
echo ==========================================
echo.

REM Configuration
set RESOURCE_GROUP=opus-rg
set APP_NAME=opus-task-manager
set LOCATION=East US
set APP_SERVICE_PLAN=opus-app-plan
set SKU=B1

REM Check if Azure CLI is installed
az --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Azure CLI is not installed. Please install it first.
    echo Download from: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli
    pause
    exit /b 1
)

REM Check if user is logged in
az account show >nul 2>&1
if errorlevel 1 (
    echo [WARNING] You are not logged in to Azure. Logging in...
    az login
)

echo [SUCCESS] Azure CLI is ready!

REM Get unique app name
set /p user_app_name="Enter a unique app name (or press Enter for default 'opus-task-manager'): "
if not "!user_app_name!"=="" set APP_NAME=!user_app_name!

echo.
echo Deploying with app name: !APP_NAME!
echo.

REM Step 1: Create Resource Group
echo Step 1: Creating Resource Group
echo ================================
az group show --name !RESOURCE_GROUP! >nul 2>&1
if not errorlevel 1 (
    echo [WARNING] Resource group '!RESOURCE_GROUP!' already exists
) else (
    az group create --name !RESOURCE_GROUP! --location "!LOCATION!"
    echo [SUCCESS] Resource group created
)

REM Step 2: Create App Service Plan
echo.
echo Step 2: Creating App Service Plan
echo ==================================
az appservice plan show --name !APP_SERVICE_PLAN! --resource-group !RESOURCE_GROUP! >nul 2>&1
if not errorlevel 1 (
    echo [WARNING] App Service plan '!APP_SERVICE_PLAN!' already exists
) else (
    az appservice plan create --name !APP_SERVICE_PLAN! --resource-group !RESOURCE_GROUP! --sku !SKU! --is-linux
    echo [SUCCESS] App Service plan created
)

REM Step 3: Create Web App
echo.
echo Step 3: Creating Web App
echo =========================
az webapp show --name !APP_NAME! --resource-group !RESOURCE_GROUP! >nul 2>&1
if not errorlevel 1 (
    echo [WARNING] Web app '!APP_NAME!' already exists
) else (
    az webapp create --name !APP_NAME! --resource-group !RESOURCE_GROUP! --plan !APP_SERVICE_PLAN! --runtime "JAVA:21-java21"
    echo [SUCCESS] Web app created
)

REM Step 4: Configure Application Settings
echo.
echo Step 4: Configuring Application Settings
echo ========================================

REM Database Configuration
az webapp config appsettings set --name !APP_NAME! --resource-group !RESOURCE_GROUP! --settings SPRING_DATASOURCE_URL="jdbc:postgresql://ep-dawn-river-a1m8le9c-pooler.ap-southeast-1.aws.neon.tech/opus?user=neondb_owner&password=npg_NMZW5Vw0Gukp&sslmode=require&channelBinding=require" SPRING_DATASOURCE_USERNAME="neondb_owner" SPRING_DATASOURCE_PASSWORD="npg_NMZW5Vw0Gukp" SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver" >nul
echo [SUCCESS] Database configuration set

REM JPA Configuration
az webapp config appsettings set --name !APP_NAME! --resource-group !RESOURCE_GROUP! --settings SPRING_JPA_HIBERNATE_DDL_AUTO="update" SPRING_JPA_SHOW_SQL="false" SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT="org.hibernate.dialect.PostgreSQLDialect" >nul
echo [SUCCESS] JPA configuration set

REM JWT Configuration
az webapp config appsettings set --name !APP_NAME! --resource-group !RESOURCE_GROUP! --settings JWT_SECRET="OpusSecureJwtKey2025!@#$%^&*()_+{}|:<>?[]\\;',./-=`~1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" JWT_EXPIRATION="86400000" JWT_REFRESH_EXPIRATION="604800000" >nul
echo [SUCCESS] JWT configuration set

REM Optional: Email Configuration
echo.
echo Email Configuration (Optional)
echo ==============================
set /p configure_email="Do you want to configure email settings? (y/n): "
if /i "!configure_email!"=="y" (
    set /p gmail_username="Enter Gmail username: "
    set /p gmail_password="Enter Gmail app password: "
    
    az webapp config appsettings set --name !APP_NAME! --resource-group !RESOURCE_GROUP! --settings SPRING_MAIL_HOST="smtp.gmail.com" SPRING_MAIL_PORT="587" SPRING_MAIL_USERNAME="!gmail_username!" SPRING_MAIL_PASSWORD="!gmail_password!" SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true" SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE="true" >nul
    echo [SUCCESS] Email configuration set
)

REM Step 5: Build Application
echo.
echo Step 5: Building Application
echo =============================
if not exist "pom.xml" (
    echo [ERROR] pom.xml not found. Please run this script from the project root directory.
    pause
    exit /b 1
)

mvn clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)
echo [SUCCESS] Application built successfully

REM Step 6: Deploy Application
echo.
echo Step 6: Deploying Application
echo ==============================
az webapp deploy --name !APP_NAME! --resource-group !RESOURCE_GROUP! --src-path target/opus-0.0.1-SNAPSHOT.jar --type jar
echo [SUCCESS] Application deployed successfully!

REM Step 7: Configure Security Settings
echo.
echo Step 7: Configuring Security
echo =============================

REM Enable HTTPS only
az webapp update --name !APP_NAME! --resource-group !RESOURCE_GROUP! --https-only true >nul
echo [SUCCESS] HTTPS-only enabled

REM Configure health check
az webapp config set --name !APP_NAME! --resource-group !RESOURCE_GROUP! --health-check-path "/public/health" >nul
echo [SUCCESS] Health check configured

REM Final Information
echo.
echo ==========================================
echo    Deployment Completed Successfully!
echo ==========================================
echo.
echo Application URL: https://!APP_NAME!.azurewebsites.net
echo Health Check: https://!APP_NAME!.azurewebsites.net/public/health
echo API Documentation: https://!APP_NAME!.azurewebsites.net/swagger-ui.html
echo Resource Group: !RESOURCE_GROUP!
echo.
echo Next Steps:
echo 1. Test your application at the URL above
echo 2. Configure OAuth2 redirect URI in Google Console:
echo    https://!APP_NAME!.azurewebsites.net/login/oauth2/code/google
echo 3. Set up monitoring and alerts
echo 4. Configure custom domain (optional)
echo.
echo To view logs:
echo az webapp log tail --name !APP_NAME! --resource-group !RESOURCE_GROUP!
echo.
echo To update the application:
echo 1. mvn clean package -DskipTests
echo 2. az webapp deploy --name !APP_NAME! --resource-group !RESOURCE_GROUP! --src-path target/opus-0.0.1-SNAPSHOT.jar --type jar
echo.

REM Optional: Open browser
set /p open_browser="Do you want to open the application in your browser? (y/n): "
if /i "!open_browser!"=="y" (
    start https://!APP_NAME!.azurewebsites.net
)

echo.
echo Deployment script completed!
pause
