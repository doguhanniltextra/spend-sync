# =============================================================================
# SPENDSYNC RDS MODULE - OUTPUTS
# =============================================================================

output "db_instance_endpoint" {
  value       = aws_db_instance.postgres.endpoint
  description = "RDS PostgreSQL connection endpoint (host:port)"
}

output "db_instance_address" {
  value       = aws_db_instance.postgres.address
  description = "RDS PostgreSQL host address"
}

output "db_name" {
  value       = aws_db_instance.postgres.db_name
  description = "PostgreSQL database name"
}

output "master_password_secret_arn" {
  value       = aws_db_instance.postgres.master_user_secret[0].secret_arn
  description = "AWS Secrets Manager secret ARN for the master password"
}
