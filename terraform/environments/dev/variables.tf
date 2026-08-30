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

variable "frontend_bucket_name" {
  description = "must be globally unique"
}

variable "frontend_url" {
  default = "http://localhost:5173"
}

variable "backend_image_tag" {
  description = "Image tag to deploy to dev, e.g. a git commit SHA or 'latest'"
  default     = "latest"
}

variable "ecs_desired_count" {
  description = "Set to 0 to stop the container and avoid Fargate costs when not testing."
  type        = number
  default     = 1
}
