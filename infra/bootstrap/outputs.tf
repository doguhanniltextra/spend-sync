# -----------------------------------------------------------------------------
# AWS Bootstrap Output Definitions
# -----------------------------------------------------------------------------

# 1. S3 Remote State Bucket Name
# Configured in AWS-03 environment backend definitions (infra/envs/dev/backend.tf).
output "s3_state_bucket_name" {
  value       = aws_s3_bucket.tf_state.id
  description = "Terraform remote state storage S3 bucket name"
}

# 2. GitHub Actions Access Key ID
# Configured in GitHub Repository Settings -> Secrets -> AWS_ACCESS_KEY_ID.
output "github_actions_access_key_id" {
  value       = aws_iam_access_key.github_deployer_key.id
  description = "IAM Access Key ID for GitHub Actions CI/CD (AWS_ACCESS_KEY_ID)"
}

# 3. GitHub Actions Secret Access Key
# Configured in GitHub Repository Settings -> Secrets -> AWS_SECRET_ACCESS_KEY (Sensitive Value).
output "github_actions_secret_access_key" {
  value       = aws_iam_access_key.github_deployer_key.secret
  sensitive   = true
  description = "IAM Secret Access Key for GitHub Actions CI/CD (AWS_SECRET_ACCESS_KEY)"
}

# 4. Amazon ECR Repository URL
# Configured in GitHub Repository Settings -> Variables -> ECR_REPOSITORY_URL.
output "ecr_repository_url" {
  value       = aws_ecr_repository.backend.repository_url
  description = "ECR Repository URL for spendsync-backend Docker images"
}
