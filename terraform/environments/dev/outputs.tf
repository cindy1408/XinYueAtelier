output "rds_endpoint" {
  value = module.atelier.rds_endpoint
}

output "s3_bucket" {
  value = module.atelier.s3_bucket
}

output "ecs_cluster_name" {
  value = module.atelier.ecs_cluster_name
}

output "ecs_service_name" {
  value = module.atelier.ecs_service_name
}
