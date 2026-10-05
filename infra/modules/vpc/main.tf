# =============================================================================
# VIRTUAL PRIVATE CLOUD (VPC) & NETWORKING MODULE ($0 NAT GATEWAY DESIGN)
# =============================================================================

# Fetch available Availability Zones in current region (eu-north-1a, eu-north-1b)
data "aws_availability_zones" "available" {
  state = "available"
}

# Main VPC Resource
# Enables DNS hostnames and DNS support required for ECS service discovery and SSM endpoints.
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "spendsync-${var.environment}-vpc"
  }
}

# Internet Gateway (IGW)
# Connects the VPC to the public internet for public subnets.
resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "spendsync-${var.environment}-igw"
  }
}

# Public Subnets (2 AZs)
# Hosts ALB and ECS Fargate Spot tasks.
# map_public_ip_on_launch = true enables public IP assignment without NAT Gateway.
resource "aws_subnet" "public" {
  count                   = length(var.public_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "spendsync-${var.environment}-public-subnet-${count.index + 1}"
    Type = "Public"
  }
}

# Private Subnets (2 AZs)
# Hosts RDS PostgreSQL database and ElastiCache Valkey instances.
# Completely isolated from public internet access.
resource "aws_subnet" "private" {
  count                   = length(var.private_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.private_subnet_cidrs[count.index]
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = false

  tags = {
    Name = "spendsync-${var.environment}-private-subnet-${count.index + 1}"
    Type = "Private"
  }
}

# Public Route Table & Associations
# Routes all outbound traffic (0.0.0.0/0) to the Internet Gateway.
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gw.id
  }

  tags = {
    Name = "spendsync-${var.environment}-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Private Route Table & Associations
# Contains no default route to Internet Gateway or NAT Gateway (Local VPC traffic only).
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "spendsync-${var.environment}-private-rt"
  }
}

resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
