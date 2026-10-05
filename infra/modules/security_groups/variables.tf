# -----------------------------------------------------------------------------
# Security Groups Module Variable Definitions
# -----------------------------------------------------------------------------

# VPC ID where security groups will be provisioned
variable "vpc_id" {
  type        = string
  description = "VPC ID where the security groups will be created"
}

# Target Environment Name
variable "environment" {
  type        = string
  default     = "dev"
  description = "Deployment target environment name (e.g. dev, prod)"
}
