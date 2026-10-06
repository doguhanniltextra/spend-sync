# =============================================================================
# SPENDSYNC ECS MODULE - OUTPUTS
# =============================================================================

output "cluster_name" {
  value       = aws_ecs_cluster.main.name
  description = "ECS Cluster Name"
}

output "cluster_id" {
  value       = aws_ecs_cluster.main.id
  description = "ECS Cluster ID"
}

output "execution_role_arn" {
  value       = aws_iam_role.execution_role.arn
  description = "ECS Task Execution Role ARN"
}

output "service_name" {
  value       = aws_ecs_service.backend.name
  description = "ECS Service Name"
}

output "task_definition_arn" {
  value       = aws_ecs_task_definition.backend.arn
  description = "ECS Task Definition ARN"
}

output "cloudwatch_log_group_name" {
  value       = aws_cloudwatch_log_group.ecs_logs.name
  description = "CloudWatch Log Group Name for ECS container logs"
}

output "service_connect_namespace_arn" {
  value       = aws_service_discovery_private_dns_namespace.main.arn
  description = "ECS Service Connect Private DNS Namespace ARN"
}
