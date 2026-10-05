# -----------------------------------------------------------------------------
# AWS Bootstrap Variable Definitions
# -----------------------------------------------------------------------------

# AWS Region: Stockholm (eu-north-1)
# All infrastructure resources are provisioned in this region based on cost and latency requirements.
variable "aws_region" {
  type        = string
  default     = "eu-north-1"
  description = "The default AWS Region (Stockholm) where all infrastructure resources will be deployed"
}

# AWS CLI Profile Name: spendsync
# Specifies the administrator AWS CLI profile name configured in ~/.aws/credentials.
variable "aws_profile" {
  type        = string
  default     = "spendsync"
  description = "The authorized AWS CLI profile name used for local command execution"
}

# GitHub Repository Name
# Referenced in CI/CD access permissions, trust policies, and documentation links.
variable "github_repo" {
  type        = string
  default     = "doguhanniltextra/spend-sync"
  description = "The full repository name on GitHub formatted as 'owner/repository'"
}
