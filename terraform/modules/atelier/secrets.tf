resource "aws_secretsmanager_secret" "db_password" {
  count = var.enable_backend ? 1 : 0
  name  = "${var.name_prefix}/db-password"
}

resource "aws_secretsmanager_secret_version" "db_password" {
  count         = var.enable_backend ? 1 : 0
  secret_id     = aws_secretsmanager_secret.db_password[0].id
  secret_string = var.db_password
}

resource "aws_secretsmanager_secret" "google_client_secret" {
  count = var.enable_backend ? 1 : 0
  name  = "${var.name_prefix}/google-client-secret"
}

resource "aws_secretsmanager_secret_version" "google_client_secret" {
  count         = var.enable_backend ? 1 : 0
  secret_id     = aws_secretsmanager_secret.google_client_secret[0].id
  secret_string = var.google_client_secret
}
