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
