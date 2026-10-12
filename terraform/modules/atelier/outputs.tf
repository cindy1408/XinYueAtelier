output "rds_endpoint" {
  value = var.enable_backend ? aws_db_instance.atelier[0].address : null
}

output "s3_bucket" {
  value = aws_s3_bucket.atelier.bucket
}


output "ecs_cluster_name" {
  value = var.enable_backend ? aws_ecs_cluster.atelier[0].name : null
}

output "ecs_service_name" {
  value = var.enable_backend ? aws_ecs_service.atelier[0].name : null
}

output "alb_dns_name" {
  value = var.enable_alb ? aws_lb.atelier[0].dns_name : null
}

output "api_url" {
  value = var.enable_alb ? "https://${var.api_domain_name}" : null
}

output "frontend_bucket_id" {
  value = aws_s3_bucket.frontend.id
}

output "frontend_bucket_arn" {
  value = aws_s3_bucket.frontend.arn
}

output "frontend_bucket_regional_domain_name" {
  value = aws_s3_bucket.frontend.bucket_regional_domain_name
}