variable "environment" {
  type        = string
  description = "Target deployment environment (e.g., dev, prod)"
  default     = "dev"
}

variable "aws_account_id" {
  type        = string
  description = "AWS Account ID used to uniquely namespace S3 bucket names"
}

variable "alb_dns_name" {
  type        = string
  description = "Public DNS name of the Application Load Balancer to route /api/* requests to"
  default     = ""
}
