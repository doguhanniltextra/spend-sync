# -----------------------------------------------------------------------------
# Prod Environment AWS Provider & Remote State Configuration
# -----------------------------------------------------------------------------

terraform {
  required_version = ">= 1.11.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket       = "spendsync-tf-state-783582650549-eu-north-1"
    key          = "envs/prod/terraform.tfstate"
    region       = "eu-north-1"
    profile      = "spendsync"
    use_lockfile = true
  }
}

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = {
      Project     = "SpendSync"
      Environment = "prod"
      ManagedBy   = "Terraform"
    }
  }
}
