# -----------------------------------------------------------------------------
# VPC Module Variable Definitions
# -----------------------------------------------------------------------------

# Primary CIDR block for the SpendSync VPC network
variable "vpc_cidr" {
  type        = string
  default     = "10.0.0.0/16"
  description = "CIDR block for the SpendSync VPC network"
}

# CIDR blocks for Public Subnets across 2 Availability Zones
# Hosting ALB and ECS Fargate Spot tasks (assign_public_ip = true for zero-NAT Gateway costs)
variable "public_subnet_cidrs" {
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
  description = "List of CIDR blocks for public subnets across 2 Availability Zones"
}

# CIDR blocks for Private Subnets across 2 Availability Zones
# Hosting RDS PostgreSQL and ElastiCache Valkey instances (completely isolated from public internet)
variable "private_subnet_cidrs" {
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
  description = "List of CIDR blocks for private subnets across 2 Availability Zones"
}

# Target Environment Name
variable "environment" {
  type        = string
  default     = "dev"
  description = "Deployment target environment name (e.g. dev, prod)"
}
