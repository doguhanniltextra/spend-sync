# =============================================================================
# SPENDSYNC ECS MODULE - ROUTE 53 PRIVATE DNS SERVICE DISCOVERY
# =============================================================================

resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "spendsync.local"
  description = "ECS Service Connect & Discovery Private DNS namespace for SpendSync services"
  vpc         = var.vpc_id

  tags = {
    Name        = "spendsync-${var.environment}-service-connect"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# Cloud Map Service Discovery Service with Route 53 A-record
resource "aws_service_discovery_service" "backend" {
  name = "backend"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }

  tags = {
    Name        = "spendsync-${var.environment}-backend-sd"
    Environment = var.environment
    Project     = "SpendSync"
  }
}
