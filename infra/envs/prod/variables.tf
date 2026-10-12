# -----------------------------------------------------------------------------
# Prod Environment Variable Definitions
# -----------------------------------------------------------------------------

variable "aws_region" {
  type        = string
  default     = "eu-north-1"
  description = "Target AWS Region"
}

variable "aws_profile" {
  type        = string
  default     = "spendsync"
  description = "AWS CLI profile name"
}

variable "environment" {
  type        = string
  default     = "prod"
  description = "Target environment name"
}

variable "ecr_repository_url" {
  type        = string
  default     = "783582650549.dkr.ecr.eu-north-1.amazonaws.com/spendsync-backend"
  description = "ECR repository URL for spendsync-backend"
}

variable "alert_email" {
  type        = string
  default     = ""
  description = "Recipient email address for AWS Budget notifications"
}

variable "monthly_budget_limit" {
  type        = string
  default     = "50"
  description = "Total monthly budget threshold in USD"
}

variable "rds_budget_limit" {
  type        = string
  default     = "20"
  description = "Monthly RDS expenditure threshold in USD"
}

variable "ecs_budget_limit" {
  type        = string
  default     = "15"
  description = "Monthly ECS compute expenditure threshold in USD"
}

variable "sender_email" {
  type        = string
  default     = ""
  description = "SES verified sender email address for Lambda budget alerts"
}
