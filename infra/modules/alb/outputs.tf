# =============================================================================
# SPENDSYNC ALB MODULE - OUTPUTS
# =============================================================================

output "alb_dns_name" {
  value       = aws_lb.main.dns_name
  description = "Public DNS domain name of the Application Load Balancer"
}

output "alb_arn" {
  value       = aws_lb.main.arn
  description = "ARN of the Application Load Balancer"
}

output "target_group_arn" {
  value       = aws_lb_target_group.backend.arn
  description = "ARN of the ECS Target Group"
}
