# =============================================================================
# SPENDSYNC OBSERVABILITY - ECS FARGATE SPOT SERVICES & SERVICE CONNECT
# =============================================================================

# 1. Prometheus ECS Service (desired_count = 1 for TSDB lock protection)
resource "aws_ecs_service" "prometheus" {
  name            = "spendsync-${var.environment}-prometheus"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.prometheus.arn
  desired_count   = 1

  # INC-012: Prevent TSDB lock contention on shared EFS persistent volume during deployments
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
    base              = 0
  }

  network_configuration {
    subnets          = var.public_subnet_ids
    security_groups  = [aws_security_group.prometheus.id]
    assign_public_ip = var.assign_public_ip
  }

  service_connect_configuration {
    enabled   = true
    namespace = var.service_connect_namespace_arn
    service {
      port_name      = "prometheus-http"
      discovery_name = "prometheus"
      client_alias {
        port     = 9090
        dns_name = "prometheus"
      }
    }
  }

  lifecycle {
    ignore_changes = [task_definition]
  }

  tags = {
    Name        = "spendsync-${var.environment}-prometheus-service"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 2. Grafana ECS Service
resource "aws_ecs_service" "grafana" {
  name            = "spendsync-${var.environment}-grafana"
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.grafana.arn
  desired_count   = 1

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
    base              = 0
  }

  network_configuration {
    subnets          = var.public_subnet_ids
    security_groups  = [aws_security_group.grafana.id]
    assign_public_ip = var.assign_public_ip
  }

  service_connect_configuration {
    enabled   = true
    namespace = var.service_connect_namespace_arn
    service {
      port_name      = "grafana-http"
      discovery_name = "grafana"
      client_alias {
        port     = 3000
        dns_name = "grafana"
      }
    }
  }

  lifecycle {
    ignore_changes = [task_definition]
  }

  tags = {
    Name        = "spendsync-${var.environment}-grafana-service"
    Environment = var.environment
    Project     = "SpendSync"
  }
}
