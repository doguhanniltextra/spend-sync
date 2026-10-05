# =============================================================================
# SPENDSYNC ECS MODULE - MAIN RESOURCES
# =============================================================================

data "aws_region" "current" {}
data "aws_caller_identity" "current" {}

# 1. ECS Cluster & Fargate Spot Capacity Provider
resource "aws_ecs_cluster" "main" {
  name = "spendsync-${var.environment}-cluster"

  setting {
    name  = "containerInsights"
    value = "disabled"
  }

  tags = {
    Name        = "spendsync-${var.environment}-cluster"
    Environment = var.environment
  }
}

resource "aws_ecs_cluster_capacity_providers" "spot" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE_SPOT", "FARGATE"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
    base              = 0
  }
}

# 2. CloudWatch Log Group for ECS Tasks
resource "aws_cloudwatch_log_group" "ecs_logs" {
  name              = "/ecs/spendsync-backend-${var.environment}"
  retention_in_days = 14

  tags = {
    Name        = "spendsync-${var.environment}-ecs-logs"
    Environment = var.environment
  }
}

# 3. IAM Roles (Task Execution Role & Task Role)
data "aws_iam_policy_document" "ecs_task_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution_role" {
  name               = "spendsync-ecs-execution-role-${var.environment}"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = {
    Name        = "spendsync-ecs-execution-role-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_iam_role_policy_attachment" "execution_role_policy" {
  role       = aws_iam_role.execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Inline policy to allow reading SecureString parameters from SSM Parameter Store
resource "aws_iam_role_policy" "ssm_read_policy" {
  name = "spendsync-ecs-ssm-read-policy"
  role = aws_iam_role.execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ssm:GetParameters", "ssm:GetParameter"]
        Resource = "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/spendsync/${var.environment}/*"
      }
    ]
  })
}

resource "aws_iam_role" "task_role" {
  name               = "spendsync-ecs-task-role-${var.environment}"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = {
    Name        = "spendsync-ecs-task-role-${var.environment}"
    Environment = var.environment
  }
}

# 4. ECS Task Definition (ARM64 Graviton)
resource "aws_ecs_task_definition" "backend" {
  family                   = "spendsync-backend-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.execution_role.arn
  task_role_arn            = aws_iam_role.task_role.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64"
  }

  container_definitions = jsonencode([
    {
      name      = "spendsync-backend"
      image     = "${var.ecr_repository_url}:latest"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${var.db_endpoint}/spendsync_db?sslmode=require" },
        { name = "SPRING_DATASOURCE_USERNAME", value = "spendsync" },
        { name = "REDIS_HOST", value = var.valkey_endpoint },
        { name = "REDIS_PORT", value = "6379" },
        { name = "REDIS_SSL_ENABLED", value = "true" },
        { name = "CORS_ALLOWED_ORIGINS", value = "*" }
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/spendsync/${var.environment}/database-password"
        },
        {
          name      = "JWT_SECRET"
          valueFrom = "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/spendsync/${var.environment}/jwt-secret"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_logs.name
          "awslogs-region"        = data.aws_region.current.name
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  tags = {
    Name        = "spendsync-backend-${var.environment}"
    Environment = var.environment
  }
}

# 5. ECS Service (Fargate Spot & ALB Target Group Integration)
resource "aws_ecs_service" "backend" {
  name            = "spendsync-backend-${var.environment}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 1

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
    base              = 0
  }

  network_configuration {
    subnets          = var.public_subnet_ids
    security_groups  = [var.ecs_security_group_id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "spendsync-backend"
    container_port   = 8080
  }

  lifecycle {
    ignore_changes = [task_definition]
  }

  tags = {
    Name        = "spendsync-backend-${var.environment}-service"
    Environment = var.environment
  }
}
