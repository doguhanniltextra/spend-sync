# =============================================================================
# SPENDSYNC OBSERVABILITY & MONITORING MODULE - OUTPUTS
# =============================================================================

output "efs_file_system_id" {
  value       = aws_efs_file_system.monitoring.id
  description = "EFS File System ID for monitoring data"
}

output "efs_file_system_arn" {
  value       = aws_efs_file_system.monitoring.arn
  description = "EFS File System ARN"
}

output "prometheus_access_point_id" {
  value       = aws_efs_access_point.prometheus.id
  description = "EFS Access Point ID for Prometheus TSDB"
}

output "grafana_access_point_id" {
  value       = aws_efs_access_point.grafana.id
  description = "EFS Access Point ID for Grafana state"
}

output "prometheus_security_group_id" {
  value       = aws_security_group.prometheus.id
  description = "Prometheus Security Group ID"
}

output "grafana_security_group_id" {
  value       = aws_security_group.grafana.id
  description = "Grafana Security Group ID"
}

output "efs_security_group_id" {
  value       = aws_security_group.efs.id
  description = "EFS Security Group ID"
}

output "service_connect_namespace_arn" {
  value       = var.service_connect_namespace_arn
  description = "ECS Service Connect HTTP Namespace ARN"
}

output "service_connect_namespace_name" {
  value       = "spendsync.local"
  description = "ECS Service Connect HTTP Namespace Name"
}

output "prometheus_service_name" {
  value       = aws_ecs_service.prometheus.name
  description = "Prometheus ECS Service Name"
}

output "grafana_service_name" {
  value       = aws_ecs_service.grafana.name
  description = "Grafana ECS Service Name"
}

output "prometheus_task_definition_arn" {
  value       = aws_ecs_task_definition.prometheus.arn
  description = "Prometheus ECS Task Definition ARN"
}

output "grafana_task_definition_arn" {
  value       = aws_ecs_task_definition.grafana.arn
  description = "Grafana ECS Task Definition ARN"
}
