# =============================================================================
# SPENDSYNC OBSERVABILITY & MONITORING MODULE - VARIABLES
# =============================================================================

variable "environment" {
  type        = string
  description = "Target environment identifier (e.g. dev, prod)"
}

variable "vpc_id" {
  type        = string
  description = "VPC ID where monitoring resources and EFS reside"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs for EFS mount targets"
}

variable "public_subnet_ids" {
  type        = list(string)
  default     = []
  description = "Public subnet IDs for ECS tasks ($0 NAT Gateway design)"
}

variable "ecs_cluster_id" {
  type        = string
  description = "ECS Cluster ID"
}

variable "ecs_cluster_name" {
  type        = string
  description = "ECS Cluster Name"
}

variable "execution_role_arn" {
  type        = string
  description = "ECS Task Execution Role ARN"
}

variable "backend_security_group_id" {
  type        = string
  description = "Security group ID of the backend ECS service to allow scrape traffic"
}

variable "prometheus_image" {
  type        = string
  description = "ECR Image URI for Prometheus (e.g. <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/spendsync-dev-prometheus:latest)"
}

variable "grafana_image" {
  type        = string
  description = "ECR Image URI for Grafana (e.g. <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/spendsync-dev-grafana:latest)"
}

variable "developer_ingress_cidr" {
  type        = string
  default     = "0.0.0.0/0"
  description = "Authorized CIDR block for direct Grafana UI access (port 3000)"
}

variable "grafana_admin_password" {
  type        = string
  default     = "Password123!"
  sensitive   = true
  description = "Initial admin password for Grafana UI"
}

variable "assign_public_ip" {
  type        = bool
  default     = true
  description = "Assign public IP to Fargate tasks for ECR pull in $0 NAT environments"
}

variable "service_connect_namespace_arn" {
  type        = string
  description = "ECS Service Connect HTTP namespace ARN"
}
