# =============================================================================
# SPENDSYNC RDS MODULE - VARIABLES
# =============================================================================

variable "private_subnet_ids" {
  type        = list(string)
  description = "List of private subnet IDs for DB Subnet Group"
}

variable "rds_security_group_id" {
  type        = string
  description = "Security Group ID for RDS access (rds-sg)"
}

variable "environment" {
  type        = string
  default     = "dev"
  description = "Target environment name"
}
