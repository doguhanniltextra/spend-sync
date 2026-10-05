# -----------------------------------------------------------------------------
# VPC Module Output Definitions
# -----------------------------------------------------------------------------

output "vpc_id" {
  value       = aws_vpc.main.id
  description = "The ID of the SpendSync VPC network"
}

output "public_subnet_ids" {
  value       = aws_subnet.public[*].id
  description = "List of IDs for the public subnets across 2 Availability Zones"
}

output "private_subnet_ids" {
  value       = aws_subnet.private[*].id
  description = "List of IDs for the private subnets across 2 Availability Zones"
}
