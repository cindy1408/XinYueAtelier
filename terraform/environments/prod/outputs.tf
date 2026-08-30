output "ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "rds_endpoint" {
  value = module.atelier.rds_endpoint
}

output "s3_bucket" {
  value = module.atelier.s3_bucket
}

output "frontend_website_endpoint" {
  value = module.atelier.frontend_website_endpoint
}

output "alb_dns_name" {
  value = module.atelier.alb_dns_name
}

output "api_url" {
  value = module.atelier.api_url
}
