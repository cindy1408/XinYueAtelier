variable "aws_region" {
  default = "eu-west-2"
}

variable "db_username" {
  default = "atelieruser"
}

variable "db_password" {
  sensitive = true
}

variable "google_client_id" {
  sensitive = true
}

variable "google_client_secret" {
  sensitive = true
}

variable "s3_bucket_name" {
  description = "must be globally unique"
}

variable "frontend_url" {
  default = "https://xinyueatelier.com"
}

variable "backend_image" {
  description = "ECR image URI, e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/atelier-backend:latest"
}

variable "ecs_desired_count" {
  description = "Number of ECS tasks to run. Set to 0 to stop the container and avoid Fargate costs."
  type        = number
  default     = 1
}