# --- Origin Access Control (lets CloudFront read the private S3 bucket) ---
resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "atelier-frontend-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

data "aws_cloudfront_cache_policy" "caching_optimized" {
  name = "Managed-CachingOptimized"
}

# --- CloudFront distribution ---
resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  default_root_object = "index.html"
  aliases             = ["xyatelier.com", "www.xyatelier.com"]
  web_acl_id          = "arn:aws:wafv2:us-east-1:361769567236:global/webacl/CreatedByCloudFront-5da6ccb1/d1f60235-9fbe-4ba9-a72a-5f5631031015"
  is_ipv6_enabled = true

  origin {
    domain_name              = module.atelier.frontend_bucket_regional_domain_name
    origin_id                = "frontend-s3-origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  default_cache_behavior {
    target_origin_id = "frontend-s3-origin"

    allowed_methods = [
      "GET",
      "HEAD"
    ]

    cached_methods = [
      "GET",
      "HEAD"
    ]

    viewer_protocol_policy = "redirect-to-https"

    compress = true

    cache_policy_id = data.aws_cloudfront_cache_policy.caching_optimized.id
  }

  # SPA client-side routing: S3 returns 403 (not 404) for missing keys when
  # the bucket is private via OAC, so both need to map back to index.html.
  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }

  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = "arn:aws:acm:us-east-1:361769567236:certificate/d636e363-1a37-4ed5-b94a-8c85c8e1e8a5"
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
}


# --- Bucket policy: only CloudFront (via this specific distribution) can read ---
data "aws_iam_policy_document" "frontend_cloudfront_read" {
  statement {
    sid       = "AllowCloudFrontServicePrincipalReadOnly"
    actions   = ["s3:GetObject"]
    resources = ["${module.atelier.frontend_bucket_arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.frontend.arn]
    }
  }
}

# --- Route 53 records pointing both domains at CloudFront ---
resource "aws_route53_record" "frontend_apex" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "xyatelier.com"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "frontend_www" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "www.xyatelier.com"
  type    = "CNAME"

  ttl = 300

  records = [
    aws_cloudfront_distribution.frontend.domain_name
  ]
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = module.atelier.frontend_bucket_id
  policy = data.aws_iam_policy_document.frontend_cloudfront_read.json
}

data "aws_route53_zone" "main" {
  name = "xyatelier.com"
}
