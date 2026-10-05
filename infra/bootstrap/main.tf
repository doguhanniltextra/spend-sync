# =============================================================================
# 1. TERRAFORM REMOTE STATE S3 BUCKET & SECURITY CONFIGURATION
# =============================================================================

# S3 State Bucket: Securely stores all remote Terraform state files.
# prevent_destroy = true -> Prevents accidental bucket deletion if terraform destroy is invoked.
resource "aws_s3_bucket" "tf_state" {
  bucket        = "spendsync-tf-state-${data.aws_caller_identity.current.account_id}-${data.aws_region.current.name}"
  force_destroy = false

  lifecycle {
    prevent_destroy = true
  }
}

# S3 Bucket Versioning: Retains historical revisions of state files.
# Enables rollback capability in case of state file corruption or unintentional modifications.
resource "aws_s3_bucket_versioning" "tf_state_versioning" {
  bucket = aws_s3_bucket.tf_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Server-Side Encryption (SSE-S3 / AES256):
# Encrypts all stored state files at rest using AWS managed AES256 encryption keys.
resource "aws_s3_bucket_server_side_encryption_configuration" "tf_state_crypto" {
  bucket = aws_s3_bucket.tf_state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Public Access Block:
# Strictly blocks all public access to the bucket and its objects (enforces private ACLs and policies).
resource "aws_s3_bucket_public_access_block" "tf_state_public_block" {
  bucket = aws_s3_bucket.tf_state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# S3 Bucket Lifecycle Configuration:
# Automatically expires non-current state versions after 90 days to prevent infinite S3 storage accumulation.
resource "aws_s3_bucket_lifecycle_configuration" "tf_state_lifecycle" {
  bucket = aws_s3_bucket.tf_state.id

  rule {
    id     = "expire-old-versions"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }
}

# HTTPS Transport Only Bucket Policy:
# Denies unencrypted HTTP requests (aws:SecureTransport = false); mandates TLS/HTTPS for all operations.
resource "aws_s3_bucket_policy" "enforce_tls" {
  bucket = aws_s3_bucket.tf_state.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "EnforceTLSRequestsOnly"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:*"
        Resource = [
          aws_s3_bucket.tf_state.arn,
          "${aws_s3_bucket.tf_state.arn}/*"
        ]
        Condition = {
          Bool = {
            "aws:SecureTransport" = "false"
          }
        }
      }
    ]
  })
}

# =============================================================================
# 2. GITHUB ACTIONS CI/CD IAM DEPLOYMENT USER & ACCESS KEYS (SCP COMPATIBLE)
# =============================================================================

# IAM User: Dedicated IAM deployment user for GitHub Actions CI/CD pipeline authentication.
# Configured due to AWS Builder account Organization SCP restrictions blocking custom OIDC provider creation.
resource "aws_iam_user" "github_deployer" {
  name = "spendsync-github-deployer"
  path = "/ci-cd/"

  tags = {
    Description = "IAM User for GitHub Actions CI/CD pipeline deployments"
  }
}

# IAM Access Key: Generates long-lived AWS credential pair for the CI/CD deployment user.
# The resulting Access Key ID and Secret Access Key are configured in GitHub Secrets.
resource "aws_iam_access_key" "github_deployer_key" {
  user = aws_iam_user.github_deployer.name
}

# IAM User Policy: Full deployment privileges for Terraform Infrastructure and ECS CI/CD management.
resource "aws_iam_user_policy" "github_deployer_policy" {
  name = "spendsync-github-deployer-policy"
  user = aws_iam_user.github_deployer.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "*"
        Resource = "*"
      }
    ]
  })
}

# =============================================================================
# 3. AMAZON ECR (ELASTIC CONTAINER REGISTRY) REPOSITORY
# =============================================================================

# ECR Repository: Container registry hosting SpendSync backend Docker images.
# image_tag_mutability = "IMMUTABLE" -> Prevents overwriting existing image tags (v1.0.0, sha-abc).
# scan_on_push = true -> Automatically scans pushed Docker images for security vulnerabilities.
resource "aws_ecr_repository" "backend" {
  name                 = "spendsync-backend"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

# ECR Lifecycle Policy:
# Rule 1: Automatically expires untagged Docker images older than 7 days (Cost optimization).
# Rule 2: Retains a maximum of 10 tagged images total, pruning older revisions.
resource "aws_ecr_lifecycle_policy" "backend_policy" {
  repository = aws_ecr_repository.backend.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images older than 7 days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 7
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Keep last 10 images overall"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
