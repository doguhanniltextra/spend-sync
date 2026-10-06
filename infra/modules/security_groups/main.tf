# =============================================================================
# DEFENSE-IN-DEPTH SECURITY GROUPS ARCHITECTURE
# =============================================================================

# 1. Application Load Balancer Security Group (alb-sg)
# Accepts public HTTP (port 80) and HTTPS (port 443) traffic from the internet.
# Outbound traffic is restricted to forwarding port 8080 to the ECS container tasks.
resource "aws_security_group" "alb" {
  name        = "spendsync-${var.environment}-alb-sg"
  description = "Security group for public Application Load Balancer"
  vpc_id      = var.vpc_id

  ingress {
    description = "Allow HTTP inbound from public internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Allow HTTPS inbound from public internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "spendsync-${var.environment}-alb-sg"
  }
}

# 2. ECS Fargate Container Security Group (ecs-sg)
# Accepts HTTP 8080 strictly from the ALB Security Group (alb-sg).
# Outbound traffic is permitted for all protocols (DNS resolution port 53, ECR/SSM HTTPS 443, RDS 5432, Valkey 6379).
resource "aws_security_group" "ecs" {
  name        = "spendsync-${var.environment}-ecs-sg"
  description = "Security group for ECS Fargate Spring Boot container tasks"
  vpc_id      = var.vpc_id

  egress {
    description = "Allow all outbound traffic (DNS 53, HTTPS 443, RDS 5432, Valkey 6379)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "spendsync-${var.environment}-ecs-sg"
  }
}

resource "aws_security_group_rule" "alb_to_ecs_ingress" {
  type                     = "ingress"
  description              = "Allow HTTP 8080 strictly from ALB Security Group"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  security_group_id        = aws_security_group.ecs.id
  source_security_group_id = aws_security_group.alb.id
}

# Rule allowing ALB Security Group outbound on 8080 to ECS Security Group
resource "aws_security_group_rule" "alb_to_ecs" {
  type                     = "egress"
  description              = "Allow ALB outbound 8080 to ECS container tasks"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  security_group_id        = aws_security_group.alb.id
  source_security_group_id = aws_security_group.ecs.id
}

# 3. RDS PostgreSQL Security Group (rds-sg)
# Accepts PostgreSQL 5432 strictly from the ECS Container Security Group (ecs-sg).
# No direct internet access allowed.
resource "aws_security_group" "rds" {
  name        = "spendsync-${var.environment}-rds-sg"
  description = "Security group for RDS PostgreSQL database"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Allow PostgreSQL 5432 strictly from ECS Security Group"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  tags = {
    Name = "spendsync-${var.environment}-rds-sg"
  }
}

# 4. ElastiCache Valkey Security Group (valkey-sg)
# Accepts Valkey TLS 6379 strictly from the ECS Container Security Group (ecs-sg).
# No direct internet access allowed.
resource "aws_security_group" "valkey" {
  name        = "spendsync-${var.environment}-valkey-sg"
  description = "Security group for ElastiCache Valkey Serverless cache"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Allow Valkey 6379 strictly from ECS Security Group"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  tags = {
    Name = "spendsync-${var.environment}-valkey-sg"
  }
}
