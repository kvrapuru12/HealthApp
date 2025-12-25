aws_region  = "us-east-1"
db_password = "HealthApp2024!SecurePassword123"
jwt_secret  = "a2c62a62ee714b602644a39d7ff0787039df81e5c41688772164fd856d20c002"
environment = "production"

# MVP Cost Optimizations
ecs_desired_count        = 1  # Reduced from 2 (saves ~$18/month)
rds_backup_retention_days = 1  # Reduced from 7 (saves ~$0.15/month)
