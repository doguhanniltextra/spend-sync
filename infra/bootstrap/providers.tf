# -----------------------------------------------------------------------------
# AWS Provider & Terraform Configuration
# -----------------------------------------------------------------------------

terraform {
  # Minimum Terraform version v1.11.0 is required for GA S3 native locking support (use_lockfile = true).
  required_version = ">= 1.11.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# AWS Provider Configuration
# Region and CLI profile are dynamically populated from variables.
# default_tags ensures consistent resource tagging across all bootstrap components.
provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = {
      Project     = "SpendSync"
      Environment = "bootstrap"
      ManagedBy   = "Terraform"
    }
  }
}

# Dynamic Data Lookups (Data Sources)
# Prevents hardcoded Account IDs and Region references.
# `data.aws_caller_identity.current.account_id` -> Dynamically fetches current AWS Account ID (783582650549).
# `data.aws_region.current.name` -> Dynamically fetches current target region (eu-north-1).
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
