variable "environment" {
  description = "Environment name, e.g. dev or prod. Used for tagging and Spring profile selection."
  type        = string
}

variable "name_prefix" {
  description = "Prefix used in every AWS resource name. Use 'atelier' for prod (this MUST match your existing resource names exactly, or Terraform will try to destroy/recreate them). Use something like 'atelier-dev' for any other environment."
  type        = string
}

variable "aws_region" {
  type    = string
  default = "eu-west-2"
}

variable "db_username" {
  type    = string
  default = "atelieruser"
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "google_client_id" {
  type      = string
  sensitive = true
}

variable "google_client_secret" {
  type      = string
  sensitive = true
}

variable "s3_bucket_name" {
  description = "Globally unique S3 bucket name for app data (patterns/folders)."
  type        = string
}

variable "frontend_bucket_name" {
  description = "Globally unique S3 bucket name for the frontend static site."
  type        = string
}

variable "frontend_url" {
  type = string
}

variable "backend_image" {
  description = "Full ECR image URI including tag, e.g. 123456789.dkr.ecr.eu-west-2.amazonaws.com/atelier-backend:latest"
  type        = string
}

variable "ecs_desired_count" {
  type    = number
  default = 1
}

variable "db_instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "rds_deletion_protection" {
  description = "AWS-level guard: when true, the RDS API itself refuses delete requests, regardless of what Terraform tries to do."
  type        = bool
  default     = false
}

variable "rds_skip_final_snapshot" {
  type    = bool
  default = true
}

variable "rds_backup_retention_period" {
  type    = number
  default = 1
}

variable "enable_alb" {
  description = "If true, creates ALB + ACM cert + Route53 record and routes HTTPS traffic through it. If false, the ECS task is reachable directly via its own public IP on port 8080 (cheaper/simpler, good for an ephemeral dev environment)."
  type        = bool
  default     = true
}

variable "api_domain_name" {
  description = "Only used when enable_alb = true."
  type        = string
  default     = ""
}

variable "route53_zone_name" {
  type    = string
  default = "xyatelier.com"
}
