# ECS 
moved {
  from = aws_ecs_cluster.atelier
  to   = aws_ecs_cluster.atelier[0]
}

moved {
  from = aws_security_group.ecs_tasks
  to   = aws_security_group.ecs_tasks[0]
}

moved {
  from = aws_cloudwatch_log_group.atelier
  to   = aws_cloudwatch_log_group.atelier[0]
}


moved {
  from = aws_ecs_task_definition.atelier
  to   = aws_ecs_task_definition.atelier[0]
}

moved {
  from = aws_ecs_service.atelier
  to   = aws_ecs_service.atelier[0]
}

# IAM

moved {
  from = aws_iam_role.ecs_task_role
  to   = aws_iam_role.ecs_task_role[0]
}

moved {
  from = aws_iam_role_policy.s3_access
  to   = aws_iam_role_policy.s3_access[0]
}

moved {
  from = aws_iam_role.ecs_execution_role
  to   = aws_iam_role.ecs_execution_role[0]
}

moved {
  from = aws_iam_role_policy_attachment.ecs_execution
  to   = aws_iam_role_policy_attachment.ecs_execution[0]
}

moved {
  from = aws_iam_role_policy.secrets_access
  to   = aws_iam_role_policy.secrets_access[0]
}

# SECRETS

moved {
  from = aws_secretsmanager_secret.db_password
  to   = aws_secretsmanager_secret.db_password[0]
}

moved {
  from = aws_secretsmanager_secret_version.db_password
  to   = aws_secretsmanager_secret_version.db_password[0]
}

moved {
  from = aws_secretsmanager_secret.google_client_secret
  to   = aws_secretsmanager_secret.google_client_secret[0]
}

moved {
  from = aws_secretsmanager_secret_version.google_client_secret
  to   = aws_secretsmanager_secret_version.google_client_secret[0]
}

# RDS

moved {
  from = aws_security_group.rds
  to   = aws_security_group.rds[0]
}


moved {
  from = aws_db_subnet_group.atelier
  to   = aws_db_subnet_group.atelier[0]
}

moved {
  from = aws_db_instance.atelier
  to   = aws_db_instance.atelier[0]
}

