resource "aws_security_group" "rds" {
  name        = "atelier-rds-sg"
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
  name       = "atelier-db-subnet-group"
  subnet_ids = data.aws_subnets.default.ids
}

resource "aws_db_instance" "atelier" {
  identifier     = "atelier-db"
  engine         = "postgres"
  engine_version = "16.14"
  instance_class = "db.t3.micro"

  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = "atelierdb"
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.atelier.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible = false

  deletion_protection  = true
  skip_final_snapshot  = false
  final_snapshot_identifier = "atelier-db-final-snapshot"

  backup_retention_period = 7

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket" "tf_state" {
  bucket = "atelier-terraform-state"
}

resource "aws_s3_bucket_versioning" "tf_state" {
  bucket = aws_s3_bucket.tf_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_dynamodb_table" "tf_lock" {
  name         = "atelier-terraform-lock"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }
}