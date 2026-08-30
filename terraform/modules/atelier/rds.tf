resource "aws_security_group" "rds" {
  name        = "${var.name_prefix}-rds-sg"
  description = "Allow Postgres access from ECS tasks"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "atelier" {
  name       = "${var.name_prefix}-db-subnet-group"
  subnet_ids = data.aws_subnets.default.ids
}

resource "aws_db_instance" "atelier" {
  identifier     = "${var.name_prefix}-db"
  engine         = "postgres"
  engine_version = "16.14"
  instance_class = var.db_instance_class

  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = "atelierdb"
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.atelier.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible = false

  deletion_protection       = var.rds_deletion_protection
  skip_final_snapshot       = var.rds_skip_final_snapshot
  final_snapshot_identifier = var.rds_skip_final_snapshot ? null : "${var.name_prefix}-db-final-snapshot"

  backup_retention_period = var.rds_backup_retention_period

  # NOTE: `lifecycle { prevent_destroy = true }` is intentionally NOT set here.
  # Terraform requires prevent_destroy to be a literal true/false, so it can't
  # vary between dev and prod in a shared module. Protection instead comes from
  # `deletion_protection` above, which is an AWS-API-level guard (parameterizable)
  # rather than a Terraform-level one. For prod, set rds_deletion_protection = true.
}
