# =============================================================================
# SPENDSYNC BUDGET & COST ALERTING MODULE - SNS TOPIC & POLICY (AWS-01)
# =============================================================================

# 1. Amazon SNS Topic for Budget Notifications
resource "aws_sns_topic" "budget_alerts" {
  name = "spendsync-${var.environment}-budget-alerts"

  tags = {
    Name        = "spendsync-${var.environment}-budget-alerts"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 2. SNS Topic Policy Allowing AWS Budgets to Publish
resource "aws_sns_topic_policy" "budget_alerts" {
  arn = aws_sns_topic.budget_alerts.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowBudgetsPublishToTopic"
        Effect = "Allow"
        Principal = {
          Service = "budgets.amazonaws.com"
        }
        Action   = "SNS:Publish"
        Resource = aws_sns_topic.budget_alerts.arn
      }
    ]
  })
}

# 3. Direct Email Subscription (Fallback)
resource "aws_sns_topic_subscription" "email" {
  count     = var.alert_email != "" ? 1 : 0
  topic_arn = aws_sns_topic.budget_alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# =============================================================================
# 4. AWS BUDGETS DEFINITIONS (AWS-02)
# =============================================================================

# 4.1 Total Monthly Spend Budget
resource "aws_budgets_budget" "monthly_total" {
  name         = "spendsync-${var.environment}-monthly-total"
  budget_type  = "COST"
  limit_amount = var.monthly_budget_limit
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # Warning at 80% actual spend
  notification {
    comparison_operator       = "GREATER_THAN"
    threshold                 = 80
    threshold_type            = "PERCENTAGE"
    notification_type         = "ACTUAL"
    subscriber_sns_topic_arns = [aws_sns_topic.budget_alerts.arn]
  }

  # Alert at 100% actual spend
  notification {
    comparison_operator       = "GREATER_THAN"
    threshold                 = 100
    threshold_type            = "PERCENTAGE"
    notification_type         = "ACTUAL"
    subscriber_sns_topic_arns = [aws_sns_topic.budget_alerts.arn]
  }

  # Alert when forecasted spend exceeds 100%
  notification {
    comparison_operator       = "GREATER_THAN"
    threshold                 = 100
    threshold_type            = "PERCENTAGE"
    notification_type         = "FORECASTED"
    subscriber_sns_topic_arns = [aws_sns_topic.budget_alerts.arn]
  }
}

# 4.2 RDS Service Budget
resource "aws_budgets_budget" "rds" {
  name         = "spendsync-${var.environment}-rds"
  budget_type  = "COST"
  limit_amount = var.rds_budget_limit
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  cost_filter {
    name   = "Service"
    values = ["Amazon Relational Database Service"]
  }

  notification {
    comparison_operator       = "GREATER_THAN"
    threshold                 = 90
    threshold_type            = "PERCENTAGE"
    notification_type         = "ACTUAL"
    subscriber_sns_topic_arns = [aws_sns_topic.budget_alerts.arn]
  }
}

# 4.3 ECS Service Budget
resource "aws_budgets_budget" "ecs" {
  name         = "spendsync-${var.environment}-ecs"
  budget_type  = "COST"
  limit_amount = var.ecs_budget_limit
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  cost_filter {
    name   = "Service"
    values = ["Amazon Elastic Container Service"]
  }

  notification {
    comparison_operator       = "GREATER_THAN"
    threshold                 = 90
    threshold_type            = "PERCENTAGE"
    notification_type         = "ACTUAL"
    subscriber_sns_topic_arns = [aws_sns_topic.budget_alerts.arn]
  }
}

# =============================================================================
# 5. GO LAMBDA (AWS-03)
# =============================================================================

# 5.1 CloudWatch Log Group for Lambda
resource "aws_cloudwatch_log_group" "budget_lambda" {
  name              = "/aws/lambda/spendsync-${var.environment}-budget-alert-handler"
  retention_in_days = 14

  tags = {
    Name        = "spendsync-${var.environment}-budget-lambda-logs"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 5.2 IAM Execution Role for Lambda
resource "aws_iam_role" "budget_lambda" {
  name = "spendsync-${var.environment}-budget-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })

  tags = {
    Name        = "spendsync-${var.environment}-budget-lambda-role"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

resource "aws_iam_role_policy_attachment" "budget_lambda_basic" {
  role       = aws_iam_role.budget_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# 5.3 SES Send Permission (allows Lambda to call ses:SendEmail)
resource "aws_iam_role_policy" "budget_lambda_ses" {
  name = "spendsync-${var.environment}-budget-lambda-ses"
  role = aws_iam_role.budget_lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ses:SendEmail", "ses:SendRawEmail"]
      Resource = "*"
    }]
  })
}

# 5.4 Lambda ZIP (pre-built by 'make build' inside lambda/)
data "archive_file" "budget_handler" {
  type        = "zip"
  source_file = "${path.module}/lambda/bootstrap"
  output_path = "${path.module}/lambda/budget_handler.zip"
}

# 5.5 Lambda Function (Go, arm64, provided.al2023)
resource "aws_lambda_function" "budget_alert_handler" {
  function_name    = "spendsync-${var.environment}-budget-alert-handler"
  filename         = data.archive_file.budget_handler.output_path
  source_code_hash = data.archive_file.budget_handler.output_base64sha256
  role             = aws_iam_role.budget_lambda.arn
  handler          = "bootstrap"
  runtime          = "provided.al2023"
  architectures    = ["arm64"]
  memory_size      = 128
  timeout          = 15

  environment {
    variables = {
      ENVIRONMENT  = var.environment
      ALERT_EMAIL  = var.alert_email
      SENDER_EMAIL = var.sender_email
    }
  }

  depends_on = [aws_cloudwatch_log_group.budget_lambda]

  tags = {
    Name        = "spendsync-${var.environment}-budget-alert-handler"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 5.6 Allow SNS to invoke Lambda
resource "aws_lambda_permission" "sns_invoke" {
  statement_id  = "AllowSNSInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.budget_alert_handler.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = aws_sns_topic.budget_alerts.arn
}

# 5.7 SNS → Lambda Subscription
resource "aws_sns_topic_subscription" "lambda" {
  topic_arn = aws_sns_topic.budget_alerts.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.budget_alert_handler.arn
}
