# =============================================================================
# SPENDSYNC RDS MODULE - MAIN RESOURCES
# =============================================================================

# DB Subnet Group placing RDS inside private subnets across 2 Availability Zones
resource "aws_db_subnet_group" "rds" {
  name       = "spendsync-${var.environment}-db-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name        = "spendsync-${var.environment}-db-subnet-group"
    Environment = var.environment
  }
}

# RDS PostgreSQL 16 DB Instance (Single-AZ, db.t4g.micro ARM Graviton2, gp3 20GB)
resource "aws_db_instance" "postgres" {
  identifier     = "spendsync-${var.environment}-db"
  engine         = "postgres"
  engine_version = "16.14"
  instance_class = "db.t4g.micro"

  allocated_storage      = 20
  max_allocated_storage  = 50
  storage_type           = "gp3"
  storage_encrypted      = true
  multi_az               = false
  publicly_accessible    = false
  db_subnet_group_name   = aws_db_subnet_group.rds.name
  vpc_security_group_ids = [var.rds_security_group_id]

  db_name                     = "spendsync_db"
  username                    = "spendsync"
  manage_master_user_password = true

  backup_retention_period = 1
  backup_window           = "03:00-04:00"

  maintenance_window         = "Mon:04:30-Mon:05:30"
  auto_minor_version_upgrade = true
  skip_final_snapshot        = true

  tags = {
    Name        = "spendsync-${var.environment}-db"
    Environment = var.environment
  }
}
