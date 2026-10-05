# =============================================================================
# SPENDSYNC ELASTICACHE VALKEY MODULE - MAIN RESOURCES
# =============================================================================

# ElastiCache Serverless Cache for Valkey (Redis API compatible)
resource "aws_elasticache_serverless_cache" "valkey" {
  name               = "spendsync-${var.environment}-valkey"
  engine             = "valkey"
  subnet_ids         = var.private_subnet_ids
  security_group_ids = [var.valkey_security_group_id]

  cache_usage_limits {
    data_storage {
      maximum = 1
      unit    = "GB"
    }
    ecpu_per_second {
      maximum = 1000
    }
  }

  tags = {
    Name        = "spendsync-${var.environment}-valkey"
    Environment = var.environment
  }
}
