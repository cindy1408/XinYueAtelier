resource "aws_ecs_cluster" "atelier" {
  name = "atelier-cluster"
}

resource "aws_security_group" "ecs_tasks" {
  name        = "atelier-ecs-tasks-sg"
  description = "Allow inbound app traffic from ALB only, outbound to RDS/S3/internet"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_ecs_task_definition" "atelier" {
  family                   = "atelier-task"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"

    execution_role_arn = aws_iam_role.ecs_execution_role.arn
    task_role_arn       = aws_iam_role.ecs_task_role.arn

    container_definitions = jsonencode([
    {
        name      = "atelier-backend"
        image     = var.backend_image
        essential = true

        portMappings = [
        { containerPort = 8080, hostPort = 8080 }
        ]

        environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "DB_HOST", value = aws_db_instance.atelier.address },
        { name = "DB_USERNAME", value = var.db_username },
        { name = "GOOGLE_CLIENT_ID", value = var.google_client_id },
        { name = "FRONTEND_URL", value = var.frontend_url },
        { name = "S3_BUCKET", value = aws_s3_bucket.atelier.bucket },
        { name = "AWS_REGION", value = var.aws_region }
        ]

        secrets = [
        { name = "DB_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn },
        { name = "GOOGLE_CLIENT_SECRET", valueFrom = aws_secretsmanager_secret.google_client_secret.arn }
        ]

        logConfiguration = {
        logDriver = "awslogs"
        options = {
            "awslogs-group"         = aws_cloudwatch_log_group.atelier.name
            "awslogs-region"        = var.aws_region
            "awslogs-stream-prefix" = "backend"
        }
        }
    }
    ])
}

resource "aws_cloudwatch_log_group" "atelier" {
  name              = "/ecs/atelier"
  retention_in_days = 7
}

resource "aws_ecs_service" "atelier" {
  name            = "atelier-service"
  cluster         = aws_ecs_cluster.atelier.id
  task_definition = aws_ecs_task_definition.atelier.arn
  desired_count   = var.ecs_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = true
  }

    load_balancer {
    target_group_arn = aws_lb_target_group.atelier.arn
    container_name    = "atelier-backend"
    container_port    = 8080
  }

  depends_on = [aws_lb_listener.https]
}