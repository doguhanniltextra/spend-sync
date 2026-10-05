# =============================================================================
# SPENDSYNC ALB MODULE - VARIABLES
# =============================================================================

variable "vpc_id" {
  type        = string
  description = "VPC ID where the ALB and Target Group reside"
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "List of public subnet IDs for ALB placement"
}

variable "alb_security_group_id" {
  type        = string
  description = "Security Group ID for the ALB (alb-sg)"
}

variable "environment" {
  type        = string
  default     = "dev"
  description = "Target environment name"
}
