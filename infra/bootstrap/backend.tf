# -----------------------------------------------------------------------------
# Terraform Remote State Backend Configuration
# -----------------------------------------------------------------------------

# Stores the bootstrap layer's own state file securely in S3.
# Migrated from local state via 'terraform init -migrate-state'.
terraform {
  backend "s3" {
    bucket       = "spendsync-tf-state-783582650549-eu-north-1"
    key          = "bootstrap/terraform.tfstate"
    region       = "eu-north-1"
    profile      = "spendsync"
    use_lockfile = true
  }
}
