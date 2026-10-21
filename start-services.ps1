# Inventa POS - Microservices Startup Script for Windows PowerShell
# This script opens a new PowerShell window for each microservice in the recommended order.

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    Write-Error "JWT_SECRET is missing. Set it before starting services."
    exit 1
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Inventa POS - Microservices Launcher   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# 1. Eureka Server
Write-Host "[1/7] Launching Eureka Server (Port 8761)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; Write-Host 'Eureka Server (Port 8761)' -ForegroundColor Yellow; .\mvnw.cmd spring-boot:run -pl eureka-server"
Start-Sleep -Seconds 8

# 2. API Gateway
Write-Host "[2/7] Launching API Gateway (Port 5000)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; Write-Host 'API Gateway (Port 5000)' -ForegroundColor Yellow; .\mvnw.cmd spring-boot:run -pl api-gateway"
Start-Sleep -Seconds 6

# 3. User & Org Service
Write-Host "[3/7] Launching User & Org Service (Port 8081)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; Write-Host 'User & Org Service (Port 8081)' -ForegroundColor Yellow; .\mvnw.cmd spring-boot:run -pl user-org-service"
Start-Sleep -Seconds 6

# 4. Inventory Catalog Service
Write-Host "[4/7] Launching Inventory Catalog Service (Port 8082)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; Write-Host 'Inventory Catalog Service (Port 8082)' -ForegroundColor Yellow; .\mvnw.cmd spring-boot:run -pl inventory-catalog-service"
Start-Sleep -Seconds 6

# 5. Order & Sales Service
Write-Host "[5/7] Launching Order & Sales Service (Port 8083)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; Write-Host 'Order & Sales Service (Port 8083)' -ForegroundColor Yellow; .\mvnw.cmd spring-boot:run -pl order-sales-service"
Start-Sleep -Seconds 6

# 6. Billing & Analytics Service
Write-Host "[6/7] Launching Billing & Analytics Service (Port 8084)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; Write-Host 'Billing & Analytics Service (Port 8084)' -ForegroundColor Yellow; .\mvnw.cmd spring-boot:run -pl billing-analytics-service"
Start-Sleep -Seconds 4

# 7. ML Intelligence Service (FastAPI)
Write-Host "[7/7] Launching ML Intelligence Service (Port 8000)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\POS---ML'; Write-Host 'ML Intelligence Service (Port 8000)' -ForegroundColor Yellow; uvicorn app:app --host 0.0.0.0 --port 8000"

Write-Host ""
Write-Host "All 7 backend microservices have been launched in separate terminal windows!" -ForegroundColor Cyan
Write-Host "To start the frontend, run: cd POS---System-frontend && npm run dev" -ForegroundColor Cyan
