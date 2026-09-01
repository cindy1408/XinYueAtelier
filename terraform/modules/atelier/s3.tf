resource "aws_s3_bucket" "atelier" {
  bucket = var.s3_bucket_name
}

resource "aws_s3_bucket_versioning" "atelier" {
  bucket = aws_s3_bucket.atelier.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_cors_configuration" "atelier" {
  bucket = aws_s3_bucket.atelier.id

  cors_rule {
    allowed_origins = [
      "https://xyatelier.com",
      "https://www.xyatelier.com"
    ]
    allowed_methods = ["GET", "HEAD"]
    allowed_headers = ["*"]
    expose_headers  = ["ETag"]
  }
}


resource "aws_s3_bucket_public_access_block" "atelier" {
  bucket = aws_s3_bucket.atelier.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket" "frontend" {
  bucket = var.frontend_bucket_name
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

