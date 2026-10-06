# =============================================================================
# SPENDSYNC OBSERVABILITY - ECS TASK DEFINITIONS, IAM & LOG GROUPS
# =============================================================================

data "aws_region" "current" {}

# 1. IAM Role for Monitoring Tasks (EFS Client Access)
resource "aws_iam_role" "monitoring_task_role" {
  name = "spendsync-${var.environment}-monitoring-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })

  tags = {
    Name        = "spendsync-${var.environment}-monitoring-task-role"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

resource "aws_iam_policy" "efs_client_access" {
  name        = "spendsync-${var.environment}-efs-client-access"
  description = "Allows ECS tasks to mount and write to EFS"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "elasticfilesystem:ClientMount",
        "elasticfilesystem:ClientWrite",
        "elasticfilesystem:DescribeMountTargets"
      ]
      Resource = aws_efs_file_system.monitoring.arn
    }]
  })

  tags = {
    Name        = "spendsync-${var.environment}-efs-client-access"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

resource "aws_iam_role_policy_attachment" "efs_client_attach" {
  role       = aws_iam_role.monitoring_task_role.name
  policy_arn = aws_iam_policy.efs_client_access.arn
}

# 2. CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "prometheus" {
  name              = "/ecs/spendsync-${var.environment}-prometheus"
  retention_in_days = 14

  tags = {
    Name        = "spendsync-${var.environment}-prometheus-logs"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

resource "aws_cloudwatch_log_group" "grafana" {
  name              = "/ecs/spendsync-${var.environment}-grafana"
  retention_in_days = 14

  tags = {
    Name        = "spendsync-${var.environment}-grafana-logs"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 3. Prometheus Task Definition
resource "aws_ecs_task_definition" "prometheus" {
  family                   = "spendsync-${var.environment}-prometheus"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = aws_iam_role.monitoring_task_role.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  volume {
    name = "prometheus-storage"
    efs_volume_configuration {
      file_system_id          = aws_efs_file_system.monitoring.id
      transit_encryption      = "ENABLED"
      authorization_config {
        access_point_id = aws_efs_access_point.prometheus.id
        iam             = "ENABLED"
      }
    }
  }

  container_definitions = jsonencode([{
    name      = "prometheus"
    image     = var.prometheus_image
    essential = true
    portMappings = [{
      name          = "prometheus-http"
      containerPort = 9090
      hostPort      = 9090
      protocol      = "tcp"
    }]
    mountPoints = [{
      sourceVolume  = "prometheus-storage"
      containerPath = "/prometheus"
      readOnly      = false
    }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.prometheus.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "prometheus"
      }
    }
  }])

  tags = {
    Name        = "spendsync-${var.environment}-prometheus-td"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 4. Grafana Task Definition
resource "aws_ecs_task_definition" "grafana" {
  family                   = "spendsync-${var.environment}-grafana"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = aws_iam_role.monitoring_task_role.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  volume {
    name = "grafana-storage"
    efs_volume_configuration {
      file_system_id          = aws_efs_file_system.monitoring.id
      transit_encryption      = "ENABLED"
      authorization_config {
        access_point_id = aws_efs_access_point.grafana.id
        iam             = "ENABLED"
      }
    }
  }

  container_definitions = jsonencode([{
    name      = "grafana"
    image     = var.grafana_image
    essential = true
    portMappings = [{
      name          = "grafana-http"
      containerPort = 3000
      hostPort      = 3000
      protocol      = "tcp"
    }]
    mountPoints = [{
      sourceVolume  = "grafana-storage"
      containerPath = "/var/lib/grafana"
      readOnly      = false
    }]
    environment = [
      { name = "GF_SECURITY_ADMIN_USER", value = "admin" },
      { name = "GF_SECURITY_ADMIN_PASSWORD", value = var.grafana_admin_password },
      { name = "GF_USERS_ALLOW_SIGN_UP", value = "false" }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.grafana.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "grafana"
      }
    }
  }])

  tags = {
    Name        = "spendsync-${var.environment}-grafana-td"
    Environment = var.environment
    Project     = "SpendSync"
  }
}
