# =============================================================================
# SPENDSYNC DEV ENVIRONMENT INFRASTRUCTURE ORCHESTRATION
# =============================================================================

# 1. Network & VPC Module (AWS-03_01)
module "vpc" {
  source               = "../../modules/vpc"
  vpc_cidr             = "10.0.0.0/16"
  public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnet_cidrs = ["10.0.10.0/24", "10.0.11.0/24"]
  environment          = var.environment
}

# 2. Security Groups Module (AWS-03_02)
module "security_groups" {
  source      = "../../modules/security_groups"
  vpc_id      = module.vpc.vpc_id
  environment = var.environment
}

# 3. RDS PostgreSQL Module (AWS-03_03)
module "rds" {
  source                = "../../modules/rds"
  private_subnet_ids    = module.vpc.private_subnet_ids
  rds_security_group_id = module.security_groups.rds_security_group_id
  environment           = var.environment
}

# 4. ElastiCache Valkey Module (AWS-03_04)
module "valkey" {
  source                   = "../../modules/valkey"
  private_subnet_ids       = module.vpc.private_subnet_ids
  valkey_security_group_id = module.security_groups.valkey_security_group_id
  environment              = var.environment
}

# 5. Application Load Balancer Module (AWS-03_05)
module "alb" {
  source                = "../../modules/alb"
  vpc_id                = module.vpc.vpc_id
  public_subnet_ids     = module.vpc.public_subnet_ids
  alb_security_group_id = module.security_groups.alb_security_group_id
  environment           = var.environment
}

# 6. ECS Fargate Spot Module (AWS-03_06)
module "ecs" {
  source                = "../../modules/ecs"
  public_subnet_ids     = module.vpc.public_subnet_ids
  ecs_security_group_id = module.security_groups.ecs_security_group_id
  target_group_arn      = module.alb.target_group_arn
  ecr_repository_url    = var.ecr_repository_url
  db_endpoint           = module.rds.db_instance_endpoint
  valkey_endpoint       = module.valkey.valkey_endpoint
  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
}

# Caller identity for account ID
data "aws_caller_identity" "current" {}

# 7. Frontend S3 & CloudFront Module (AWS-07)
module "frontend" {
  source         = "../../modules/frontend"
  environment    = var.environment
  aws_account_id = data.aws_caller_identity.current.account_id
  alb_dns_name   = module.alb.alb_dns_name
}

# 8. Observability & Monitoring Module (AWS Observability - Faz 04)
module "monitoring" {
  source                    = "../../modules/monitoring"
  environment               = var.environment
  vpc_id                    = module.vpc.vpc_id
  private_subnet_ids        = module.vpc.private_subnet_ids
  public_subnet_ids         = module.vpc.public_subnet_ids
  ecs_cluster_id            = module.ecs.cluster_id
  ecs_cluster_name          = module.ecs.cluster_name
  execution_role_arn        = module.ecs.execution_role_arn
  backend_security_group_id = module.security_groups.ecs_security_group_id
  prometheus_image          = var.prometheus_image
  grafana_image                 = var.grafana_image
  developer_ingress_cidr        = var.developer_ingress_cidr
  service_connect_namespace_arn = module.ecs.service_connect_namespace_arn
}
