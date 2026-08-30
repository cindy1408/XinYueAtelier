data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_route53_zone" "atelier" {
  count        = var.enable_alb ? 1 : 0
  name         = var.route53_zone_name
  private_zone = false
}
