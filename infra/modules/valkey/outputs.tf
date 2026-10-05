# =============================================================================
# SPENDSYNC ELASTICACHE VALKEY MODULE - OUTPUTS
# =============================================================================

output "valkey_endpoint" {
  value       = aws_elasticache_serverless_cache.valkey.endpoint[0].address
  description = "Valkey Serverless cache endpoint host address"
}

output "valkey_port" {
  value       = aws_elasticache_serverless_cache.valkey.endpoint[0].port
  description = "Valkey Serverless cache port (6379)"
}
