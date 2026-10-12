resource "aws_iam_role" "ecs_task_role" {
  count = var.enable_backend ? 1 : 0
  name  = "${var.name_prefix}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy" "s3_access" {
  count = var.enable_backend ? 1 : 0
  name  = "${var.name_prefix}-s3-access"
  role  = aws_iam_role.ecs_task_role[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
      Resource = "${aws_s3_bucket.atelier.arn}/*"
    }]
  })
}

# ecs_execution_role — used by ECS itself, before app even starts, to pull the
# Docker image from the registry and set up CloudWatch logging.
# ecs_task_role — used by your application code to call AWS APIs (S3). This is
# where DefaultCredentialsProvider picks up.
resource "aws_iam_role" "ecs_execution_role" {
  count = var.enable_backend ? 1 : 0
  name  = "${var.name_prefix}-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution_role[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  count      = var.enable_backend ? 1 : 0
}

resource "aws_iam_role_policy" "secrets_access" {
  count = var.enable_backend ? 1 : 0
  name  = "${var.name_prefix}-secrets-access"
  role  = aws_iam_role.ecs_execution_role[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["secretsmanager:GetSecretValue"]
      Resource = [
        aws_secretsmanager_secret.db_password[0].arn,
        aws_secretsmanager_secret.google_client_secret[0].arn
      ]
    }]
  })
}

