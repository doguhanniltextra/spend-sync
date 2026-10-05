# =============================================================================
# SPENDSYNC ALB MODULE - MAIN RESOURCES
# =============================================================================

# Public Application Load Balancer in Public Subnets
resource "aws_lb" "main" {
  name               = "spendsync-${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.alb_security_group_id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false

  tags = {
    Name        = "spendsync-${var.environment}-alb"
    Environment = var.environment
  }
}

# Target Group (target_type = "ip" for ECS Fargate awsvpc mode)
resource "aws_lb_target_group" "backend" {
  name        = "spendsync-${var.environment}-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    protocol            = "HTTP"
    port                = "8080"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 15
    matcher             = "200"
  }

  tags = {
    Name        = "spendsync-${var.environment}-tg"
    Environment = var.environment
  }
}

# HTTP Listener on Port 80
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}
