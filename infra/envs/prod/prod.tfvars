# =============================================================================
# SPENDSYNC PROD ENVIRONMENT VARIABLES
# =============================================================================

aws_region         = "eu-north-1"
aws_profile        = "spendsync"
environment        = "prod"
ecr_repository_url = "783582650549.dkr.ecr.eu-north-1.amazonaws.com/spendsync-backend"

# Budget & Cost Alerting
alert_email  = "" # Set operator email for alerts (e.g. doguhan@example.com)
sender_email = "" # Must be a verified SES sender identity
