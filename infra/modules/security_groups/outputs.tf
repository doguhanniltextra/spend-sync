# -----------------------------------------------------------------------------
# Security Groups Module Output Definitions
# -----------------------------------------------------------------------------

output "alb_security_group_id" {
  value       = aws_security_group.alb.id
  description = "Application Load Balancer Security Group ID"
}

output "ecs_security_group_id" {
  value       = aws_security_group.ecs.id
  description = "ECS Fargate Task Security Group ID"
}

output "rds_security_group_id" {
  value       = aws_security_group.rds.id
  description = "RDS PostgreSQL Database Security Group ID"
}

output "valkey_security_group_id" {
  value       = aws_security_group.valkey.id
  description = "ElastiCache Valkey Cache Security Group ID"
}
