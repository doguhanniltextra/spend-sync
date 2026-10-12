output "sns_topic_arn" {
  description = "ARN of the SNS topic for budget alerts"
  value       = aws_sns_topic.budget_alerts.arn
}

output "sns_topic_name" {
  description = "Name of the SNS topic for budget alerts"
  value       = aws_sns_topic.budget_alerts.name
}

output "monthly_budget_name" {
  description = "Name of the monthly total budget"
  value       = aws_budgets_budget.monthly_total.name
}

output "rds_budget_name" {
  description = "Name of the RDS service budget"
  value       = aws_budgets_budget.rds.name
}

output "ecs_budget_name" {
  description = "Name of the ECS service budget"
  value       = aws_budgets_budget.ecs.name
}

output "lambda_function_arn" {
  description = "ARN of the budget alert Lambda function"
  value       = aws_lambda_function.budget_alert_handler.arn
}

output "lambda_function_name" {
  description = "Name of the budget alert Lambda function"
  value       = aws_lambda_function.budget_alert_handler.function_name
}
