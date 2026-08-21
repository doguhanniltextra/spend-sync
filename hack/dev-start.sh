#!/usr/bin/env bash
set -e

echo "=========================================================="
echo "         Starting SpendSync Local Development Stack        "
echo "=========================================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# 1. Start Docker Containers
if command -v docker >/dev/null 2>&1; then
    echo "[1/3] Starting PostgreSQL and Redis containers..."
    docker compose -f "$ROOT_DIR/docker-compose.yml" up -d 2>/dev/null || docker compose up -d
else
    echo "[1/3] Docker not found. Ensure PostgreSQL (5432) and Redis (6379) are active."
fi

# 2. Run Backend
echo -e "\n[2/3] Starting Spring Boot Backend (Port 8080)..."
(cd "$ROOT_DIR/backend" && mvn spring-boot:run) &
BACKEND_PID=$!

# 3. Run Frontend
if [ -d "$ROOT_DIR/frontend" ]; then
    echo -e "\n[3/3] Starting Frontend (Port 5173)..."
    (cd "$ROOT_DIR/frontend" && npm run dev) &
    FRONTEND_PID=$!
fi

echo -e "\nSpendSync stack is launching."
echo "Swagger UI:  http://localhost:8080/swagger-ui.html"
echo "API Health:  http://localhost:8080/actuator/health"
echo "Web App:     http://localhost:5173"
echo "=========================================================="

wait $BACKEND_PID
