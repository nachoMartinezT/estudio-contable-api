#!/bin/bash
set -e

echo "=== Guida Contable - Build ==="

services=(
  "auth-service"
  "api-gateway"
  "client-service"
  "invoice-service"
  "afip-service"
  "audit-service"
  "dashboard-service"
  "document-service"
  "ledger-service"
  "mp-service"
  "notification-service"
  "report-service"
)

for service in "${services[@]}"; do
  echo "Building $service..."
  cd "$service" && ../mvnw clean package -DskipTests -q && cd ..
done

echo "Build completo."
