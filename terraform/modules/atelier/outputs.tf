output "rds_endpoint" {
  value = aws_db_instance.atelier.address
}

output "s3_bucket" {
  value = aws_s3_bucket.atelier.bucket
}

output "frontend_website_endpoint" {
  value = aws_s3_bucket_website_configuration.frontend.website_endpoint
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.atelier.name
}

output "ecs_service_name" {
  value = aws_ecs_service.atelier.name
}

output "alb_dns_name" {
  value = var.enable_alb ? aws_lb.atelier[0].dns_name : null
}

output "api_url" {
  value = var.enable_alb ? "https://${var.api_domain_name}" : null
}
