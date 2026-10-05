# =============================================================================
# SPENDSYNC ECS MODULE - VARIABLES
# =============================================================================

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public Subnet IDs for ECS task networking (assign_public_ip = true)"
}

variable "ecs_security_group_id" {
  type        = string
  description = "Security Group ID for ECS tasks (ecs-sg)"
}

variable "target_group_arn" {
  type        = string
  description = "ALB Target Group ARN for load balancer registration"
}

variable "ecr_repository_url" {
  type        = string
  description = "ECR Repository URL for spendsync-backend"
}

variable "db_endpoint" {
  type        = string
  description = "RDS PostgreSQL endpoint host:port"
}

variable "valkey_endpoint" {
  type        = string
  description = "ElastiCache Valkey endpoint host"
}

variable "environment" {
  type        = string
  default     = "dev"
  description = "Target environment name"
}
