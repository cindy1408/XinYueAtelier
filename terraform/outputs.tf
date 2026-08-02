output "rds_endpoint" {
  value = aws_db_instance.atelier.address
}

output "s3_bucket" {
  value = aws_s3_bucket.atelier.bucket
}