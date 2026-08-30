terraform {
  backend "s3" {
    bucket         = "atelier-terraform-state"
    key            = "atelier/dev/terraform.tfstate"
    region         = "eu-west-2"
    dynamodb_table = "atelier-terraform-lock"
    encrypt        = true
  }
}
