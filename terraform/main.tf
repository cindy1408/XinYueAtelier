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
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

terraform {
  backend "s3" {
    bucket         = "atelier-terraform-state"
    key            = "atelier/terraform.tfstate"
    region         = "eu-west-2"
    dynamodb_table = "atelier-terraform-lock"
    encrypt        = true
  }
}