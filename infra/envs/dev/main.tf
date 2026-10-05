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
}




