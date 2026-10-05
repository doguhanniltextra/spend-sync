output "bucket_name" {
  value       = aws_s3_bucket.frontend.id
  description = "The name of the frontend S3 bucket"
}

output "bucket_arn" {
  value       = aws_s3_bucket.frontend.arn
  description = "The ARN of the frontend S3 bucket"
}

output "cloudfront_distribution_id" {
  value       = aws_cloudfront_distribution.frontend.id
  description = "The ID of the CloudFront distribution"
}

output "cloudfront_domain_name" {
  value       = aws_cloudfront_distribution.frontend.domain_name
  description = "The domain name of the CloudFront distribution"
}

output "frontend_url" {
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
  description = "The full HTTPS URL for accessing the frontend application"
}
