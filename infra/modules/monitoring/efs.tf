# =============================================================================
# SPENDSYNC OBSERVABILITY - EFS PERSISTENT STORAGE & ACCESS POINTS
# =============================================================================

# 1. EFS File System for Persistent Monitoring Data
resource "aws_efs_file_system" "monitoring" {
  creation_token   = "spendsync-${var.environment}-monitoring-efs"
  performance_mode = "generalPurpose"
  throughput_mode  = "bursting"
  encrypted        = true

  tags = {
    Name        = "spendsync-${var.environment}-monitoring-efs"
    Environment = var.environment
    Project     = "SpendSync"
    Component   = "Observability"
  }
}

# 2. EFS Mount Targets in Each Private Subnet
resource "aws_efs_mount_target" "monitoring" {
  count           = length(var.private_subnet_ids)
  file_system_id  = aws_efs_file_system.monitoring.id
  subnet_id       = var.private_subnet_ids[count.index]
  security_groups = [aws_security_group.efs.id]
}

# 3. Access Point: Prometheus TSDB (POSIX nobody: UID 65534, GID 65534)
resource "aws_efs_access_point" "prometheus" {
  file_system_id = aws_efs_file_system.monitoring.id

  posix_user {
    uid = 65534
    gid = 65534
  }

  root_directory {
    path = "/prometheus"
    creation_info {
      owner_uid   = 65534
      owner_gid   = 65534
      permissions = "755"
    }
  }

  tags = {
    Name        = "spendsync-${var.environment}-efs-ap-prometheus"
    Environment = var.environment
    Project     = "SpendSync"
  }
}

# 4. Access Point: Grafana State (POSIX grafana: UID 472, GID 472)
resource "aws_efs_access_point" "grafana" {
  file_system_id = aws_efs_file_system.monitoring.id

  posix_user {
    uid = 472
    gid = 472
  }

  root_directory {
    path = "/grafana"
    creation_info {
      owner_uid   = 472
      owner_gid   = 472
      permissions = "755"
    }
  }

  tags = {
    Name        = "spendsync-${var.environment}-efs-ap-grafana"
    Environment = var.environment
    Project     = "SpendSync"
  }
}
