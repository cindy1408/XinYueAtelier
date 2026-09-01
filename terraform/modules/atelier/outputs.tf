output "rds_endpoint" {
  value = aws_db_instance.atelier.address
}

output "s3_bucket" {
  value = aws_s3_bucket.atelier.bucket
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

output "frontend_bucket_id" {
  value = aws_s3_bucket.frontend.id
}

output "frontend_bucket_arn" {
  value = aws_s3_bucket.frontend.arn
}

output "frontend_bucket_regional_domain_name" {
  value = aws_s3_bucket.frontend.bucket_regional_domain_name
}