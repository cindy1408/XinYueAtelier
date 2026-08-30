terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Environment = "dev"
      Project     = "atelier"
    }
  }
}

# Dev shares the same ECR repo as prod — images are the same artifact,
# just deployed to different infrastructure with different tags if you want.
data "aws_ecr_repository" "backend" {
  name = "atelier-backend"
}

module "atelier" {
  source = "../../modules/atelier"

  environment = "dev"
  name_prefix = "atelier-dev"
  aws_region  = var.aws_region

  db_username          = var.db_username
  db_password          = var.db_password
  google_client_id     = var.google_client_id
  google_client_secret = var.google_client_secret

  s3_bucket_name       = var.s3_bucket_name
  frontend_bucket_name = var.frontend_bucket_name
  frontend_url         = var.frontend_url
  backend_image        = "${data.aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
  ecs_desired_count    = var.ecs_desired_count

  db_instance_class           = "db.t3.micro"
  rds_deletion_protection     = false
  rds_skip_final_snapshot     = true
  rds_backup_retention_period = 1

  enable_alb = false # direct IP access on :8080, no ALB/ACM/Route53 for dev
}
