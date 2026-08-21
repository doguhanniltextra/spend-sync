# SpendSync Quickstart PowerShell Script

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         Starting SpendSync Local Development Stack        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Start Docker Containers if docker is installed
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "[1/3] Starting PostgreSQL and Redis containers..." -ForegroundColor Yellow
    docker compose up -d
} else {
    Write-Host "[1/3] Docker not detected. Ensure local PostgreSQL (5432) and Redis (6379) are running." -ForegroundColor DarkYellow
}

# 2. Build & Run Backend
Write-Host "`n[2/3] Starting Spring Boot Backend (Port 8080)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot/../backend'; mvn spring-boot:run"

# 3. Start Frontend if directory exists
if (Test-Path "$PSScriptRoot/../frontend") {
    Write-Host "[3/3] Starting React Frontend (Port 5173)..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot/../frontend'; npm run dev"
}

Write-Host "`nSpendSync stack is launching." -ForegroundColor Green
Write-Host "Swagger UI:  http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host "API Health:  http://localhost:8080/actuator/health" -ForegroundColor Cyan
Write-Host "Web App:     http://localhost:5173" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
