# -----------------------------------------------------------------------------
# Dev Environment Root Outputs
# -----------------------------------------------------------------------------

# Network Outputs (AWS-03_01)
output "vpc_id" {
  value       = module.vpc.vpc_id
  description = "The ID of the SpendSync dev VPC"
}

output "public_subnet_ids" {
  value       = module.vpc.public_subnet_ids
  description = "List of public subnet IDs"
}

output "private_subnet_ids" {
  value       = module.vpc.private_subnet_ids
  description = "List of private subnet IDs"
}

# Security Group Outputs (AWS-03_02)
output "alb_security_group_id" {
  value       = module.security_groups.alb_security_group_id
  description = "ALB Security Group ID"
}

output "ecs_security_group_id" {
  value       = module.security_groups.ecs_security_group_id
  description = "ECS Task Security Group ID"
}

output "rds_security_group_id" {
  value       = module.security_groups.rds_security_group_id
  description = "RDS Security Group ID"
}

output "valkey_security_group_id" {
  value       = module.security_groups.valkey_security_group_id
  description = "Valkey Security Group ID"
}

# RDS PostgreSQL Outputs (AWS-03_03)
output "db_instance_endpoint" {
  value       = module.rds.db_instance_endpoint
  description = "RDS PostgreSQL connection endpoint"
}

output "db_instance_address" {
  value       = module.rds.db_instance_address
  description = "RDS PostgreSQL host address"
}

output "db_name" {
  value       = module.rds.db_name
  description = "PostgreSQL database name"
}

output "master_password_secret_arn" {
  value       = module.rds.master_password_secret_arn
  description = "AWS Secrets Manager secret ARN for RDS master password"
}

# ElastiCache Valkey Outputs (AWS-03_04)
output "valkey_endpoint" {
  value       = module.valkey.valkey_endpoint
  description = "Valkey Serverless cache endpoint host address"
}

output "valkey_port" {
  value       = module.valkey.valkey_port
  description = "Valkey Serverless cache port"
}

# Application Load Balancer Outputs (AWS-03_05)
output "alb_dns_name" {
  value       = module.alb.alb_dns_name
  description = "Public DNS domain name of the Application Load Balancer"
}

output "alb_arn" {
  value       = module.alb.alb_arn
  description = "ARN of the Application Load Balancer"
}

output "target_group_arn" {
  value       = module.alb.target_group_arn
  description = "ARN of the ECS Target Group"
}

# ECS Fargate Spot Outputs (AWS-03_06)
output "ecs_cluster_name" {
  value       = module.ecs.cluster_name
  description = "ECS Cluster Name"
}

output "ecs_service_name" {
  value       = module.ecs.service_name
  description = "ECS Service Name"
}

output "ecs_task_definition_arn" {
  value       = module.ecs.task_definition_arn
  description = "ECS Task Definition ARN"
}

output "ecs_cloudwatch_log_group_name" {
  value       = module.ecs.cloudwatch_log_group_name
  description = "CloudWatch Log Group Name for ECS container logs"
}




