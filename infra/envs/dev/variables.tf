# -----------------------------------------------------------------------------
# Dev Environment Variable Definitions
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
  default     = "dev"
  description = "Target environment name"
}

variable "ecr_repository_url" {
  type        = string
  default     = "783582650549.dkr.ecr.eu-north-1.amazonaws.com/spendsync-backend"
  description = "ECR repository URL for spendsync-backend"
}

variable "prometheus_image" {
  type        = string
  default     = "783582650549.dkr.ecr.eu-north-1.amazonaws.com/spendsync-dev-prometheus:latest"
  description = "ECR Image URI for Prometheus"
}

variable "grafana_image" {
  type        = string
  default     = "783582650549.dkr.ecr.eu-north-1.amazonaws.com/spendsync-dev-grafana:latest"
  description = "ECR Image URI for Grafana"
}

variable "developer_ingress_cidr" {
  type        = string
  default     = "31.223.86.203/32"
  description = "Authorized CIDR block for direct Grafana UI access (port 3000)"
}

