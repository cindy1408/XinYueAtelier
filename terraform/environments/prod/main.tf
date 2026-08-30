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
      Environment = "prod"
      Project     = "atelier"
    }
  }
}

# --- Shared/bootstrap resources ---
# These are account-level, not per-environment (the Terraform state backend
# itself, and the container registry both dev and prod pull images from), so
# they stay outside the module and keep their exact original addresses.

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

resource "aws_ecr_repository" "backend" {
  name                 = "atelier-backend"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# --- The actual application stack ---

module "atelier" {
  source = "../../modules/atelier"

  environment = "prod"
  name_prefix = "atelier" # MUST stay "atelier" — matches existing live resource names exactly
  aws_region  = var.aws_region

  db_username          = var.db_username
  db_password          = var.db_password
  google_client_id     = var.google_client_id
  google_client_secret = var.google_client_secret

  s3_bucket_name       = var.s3_bucket_name
  frontend_bucket_name = "xinyueatelier-frontend"
  frontend_url         = var.frontend_url
  backend_image        = var.backend_image
  ecs_desired_count    = var.ecs_desired_count

  db_instance_class           = "db.t3.micro"
  rds_deletion_protection     = true
  rds_skip_final_snapshot     = false
  rds_backup_retention_period = 7

  enable_alb        = true
  api_domain_name   = "api.xyatelier.com"
  route53_zone_name = "xyatelier.com"
}
