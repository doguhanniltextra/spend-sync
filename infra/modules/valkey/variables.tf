# =============================================================================
# SPENDSYNC ELASTICACHE VALKEY MODULE - VARIABLES
# =============================================================================

variable "private_subnet_ids" {
  type        = list(string)
  description = "List of private subnet IDs for ElastiCache Subnet Group"
}

variable "valkey_security_group_id" {
  type        = string
  description = "Security Group ID for Valkey access (valkey-sg)"
}

variable "environment" {
  type        = string
  default     = "dev"
  description = "Target environment name"
}
