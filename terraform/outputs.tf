output "alb_dns_name" {
  description = "DNS name of the load balancer"
  value       = aws_lb.healthapp_alb.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the load balancer"
  value       = aws_lb.healthapp_alb.zone_id
}

output "ecr_repository_url" {
  description = "URL of the ECR repository"
  value       = aws_ecr_repository.healthapp.repository_url
}

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.healthapp_db.endpoint
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.healthapp_vpc.id
}

output "public_subnets" {
  description = "Public subnet IDs"
  value       = [aws_subnet.public_1a.id, aws_subnet.public_1b.id]
}

output "private_subnets" {
  description = "Private subnet IDs"
  value       = [aws_subnet.private_1a.id, aws_subnet.private_1b.id]
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.healthapp_cluster.name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.healthapp_service.name
}

output "application_urls" {
  description = "Application URLs"
  value = {
    api_base     = "http://${aws_lb.healthapp_alb.dns_name}/api"
    swagger_ui   = "http://${aws_lb.healthapp_alb.dns_name}/api/swagger-ui.html"
    health_check = "http://${aws_lb.healthapp_alb.dns_name}/api/actuator/health"
  }
} 