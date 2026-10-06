# =============================================================================
# SPENDSYNC OBSERVABILITY - SECURITY GROUPS & TRAFFIC RULES
# =============================================================================

# 1. EFS Security Group (NFS Port 2049 from Prometheus and Grafana)
resource "aws_security_group" "efs" {
  name        = "spendsync-${var.environment}-efs-monitoring-sg"
  description = "Allow NFS ingress from Prometheus and Grafana ECS tasks"
  vpc_id      = var.vpc_id

  ingress {
    description     = "NFS from Prometheus and Grafana tasks"
    from_port       = 2049
    to_port         = 2049
    protocol        = "tcp"
    security_groups = [aws_security_group.prometheus.id, aws_security_group.grafana.id]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name        = "spendsync-${var.environment}-efs-monitoring-sg"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 2. Prometheus Security Group (PromQL Port 9090 from Grafana)
resource "aws_security_group" "prometheus" {
  name        = "spendsync-${var.environment}-prometheus-sg"
  description = "Prometheus service security group"
  vpc_id      = var.vpc_id

  ingress {
    description     = "PromQL API from Grafana SG"
    from_port       = 9090
    to_port         = 9090
    protocol        = "tcp"
    security_groups = [aws_security_group.grafana.id]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name        = "spendsync-${var.environment}-prometheus-sg"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 3. Grafana Security Group (Web UI Port 3000)
resource "aws_security_group" "grafana" {
  name        = "spendsync-${var.environment}-grafana-sg"
  description = "Grafana Web UI security group"
  vpc_id      = var.vpc_id

  ingress {
    description = "Grafana Web UI ingress"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = [var.developer_ingress_cidr]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name        = "spendsync-${var.environment}-grafana-sg"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 4. Ingress Rule on Backend Security Group (Scrape Port 8080 from Prometheus)
resource "aws_security_group_rule" "backend_ingress_prometheus" {
  type                     = "ingress"
  description              = "Allow Prometheus scrape on port 8080"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.prometheus.id
  security_group_id        = var.backend_security_group_id
}
