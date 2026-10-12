variable "environment" {
  type        = string
  description = "Target deployment environment (e.g. prod, dev)"
}

variable "alert_email" {
  type        = string
  description = "Recipient email address for SNS budget notifications"
  default     = ""
}

variable "monthly_budget_limit" {
  type        = string
  description = "Total monthly budget threshold in USD"
  default     = "50"
}

variable "rds_budget_limit" {
  type        = string
  description = "Monthly RDS expenditure threshold in USD"
  default     = "20"
}

variable "ecs_budget_limit" {
  type        = string
  description = "Monthly ECS compute expenditure threshold in USD"
  default     = "15"
}

variable "sender_email" {
  type        = string
  description = "SES verified sender email address used by Lambda to send budget alerts"
  default     = ""
}
