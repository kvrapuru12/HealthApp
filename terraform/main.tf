terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.67.0"
    }
  }

  # Backend configuration - uncomment and configure for remote state
  # Uncomment after creating S3 bucket and DynamoDB table (see README.md)
  # backend "s3" {
  #   bucket         = "healthapp-terraform-state"
  #   key            = "terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "terraform-state-lock"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.app_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# Use locals for repeated values
locals {
  common_tags = {
    Project     = var.app_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# =========================================
# ✅ VPC + NETWORKING
# =========================================
# Get available availability zones
data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "healthapp_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(local.common_tags, {
    Name = "healthapp-vpc"
  })
}

resource "aws_subnet" "public_1a" {
  vpc_id                  = aws_vpc.healthapp_vpc.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true
  tags = merge(local.common_tags, {
    Name = "healthapp-public-1a"
  })
}

resource "aws_subnet" "public_1b" {
  vpc_id                  = aws_vpc.healthapp_vpc.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = data.aws_availability_zones.available.names[1]
  map_public_ip_on_launch = true
  tags = merge(local.common_tags, {
    Name = "healthapp-public-1b"
  })
}

resource "aws_subnet" "private_1a" {
  vpc_id            = aws_vpc.healthapp_vpc.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = data.aws_availability_zones.available.names[0]
  tags = merge(local.common_tags, {
    Name = "healthapp-private-1a"
  })
}

resource "aws_subnet" "private_1b" {
  vpc_id            = aws_vpc.healthapp_vpc.id
  cidr_block        = "10.0.4.0/24"
  availability_zone = data.aws_availability_zones.available.names[1]
  tags = merge(local.common_tags, {
    Name = "healthapp-private-1b"
  })
}

resource "aws_internet_gateway" "healthapp_igw" {
  vpc_id = aws_vpc.healthapp_vpc.id
  tags = merge(local.common_tags, {
    Name = "healthapp-igw"
  })
}

resource "aws_eip" "nat_eip" {
  vpc = true
  tags = merge(local.common_tags, {
    Name = "healthapp-nat-eip"
  })
}

resource "aws_nat_gateway" "healthapp_nat" {
  allocation_id = aws_eip.nat_eip.id
  subnet_id     = aws_subnet.public_1a.id
  tags = merge(local.common_tags, {
    Name = "healthapp-nat"
  })
  depends_on = [aws_internet_gateway.healthapp_igw]
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.healthapp_vpc.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.healthapp_igw.id
  }
  tags = merge(local.common_tags, {
    Name = "healthapp-public-rt"
  })
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.healthapp_vpc.id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.healthapp_nat.id
  }
  tags = merge(local.common_tags, {
    Name = "healthapp-private-rt"
  })
}

resource "aws_route_table_association" "public_1a" {
  subnet_id      = aws_subnet.public_1a.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "public_1b" {
  subnet_id      = aws_subnet.public_1b.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private_1a" {
  subnet_id      = aws_subnet.private_1a.id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "private_1b" {
  subnet_id      = aws_subnet.private_1b.id
  route_table_id = aws_route_table.private.id
}

# =========================================
# ✅ SECURITY GROUPS
# =========================================
resource "aws_security_group" "alb_sg" {
  name        = "healthapp-alb-sg"
  description = "Security group for ALB"
  vpc_id      = aws_vpc.healthapp_vpc.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = merge(local.common_tags, {
    Name = "healthapp-alb-sg"
  })
}

resource "aws_security_group" "ecs_sg" {
  name        = "healthapp-ecs-sg"
  description = "Security group for ECS tasks"
  vpc_id      = aws_vpc.healthapp_vpc.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = merge(local.common_tags, {
    Name = "healthapp-ecs-sg"
  })
}

resource "aws_security_group" "rds_sg" {
  name        = "healthapp-rds-sg"
  description = "Security group for RDS"
  vpc_id      = aws_vpc.healthapp_vpc.id

  ingress {
    description     = "MySQL from ECS tasks"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "healthapp-rds-sg"
  })
}

# =========================================
# ✅ RDS DATABASE
# =========================================
resource "aws_db_subnet_group" "healthapp_db_subnet" {
  name       = "healthapp-db-subnet"
  subnet_ids = [aws_subnet.private_1a.id, aws_subnet.private_1b.id]
  tags = merge(local.common_tags, {
    Name = "healthapp-db-subnet"
  })
}

resource "aws_db_instance" "healthapp_db" {
  identifier             = "healthapp-db"
  engine                 = "mysql"
  engine_version         = "8.0.35"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  storage_type           = "gp2"
  storage_encrypted      = true
  username               = "admin"
  password               = var.db_password
  db_name                = "healthapp"
  skip_final_snapshot    = var.environment != "production" # Keep snapshot in production
  publicly_accessible    = var.environment != "production" # Private in production
  db_subnet_group_name   = aws_db_subnet_group.healthapp_db_subnet.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]

  # Backup configuration
  backup_retention_period = var.enable_rds_backups ? var.rds_backup_retention_days : 0
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"

  # Performance insights (optional, can be enabled for production)
  performance_insights_enabled = var.environment == "production"

  # Enable deletion protection in production
  deletion_protection = var.environment == "production"

  tags = merge(local.common_tags, {
    Name = "healthapp-db"
  })
}

# =========================================
# ✅ SECRETS MANAGER (syncs with DB password)
# =========================================
resource "aws_secretsmanager_secret" "db_password" {
  name        = "healthapp/db-password"
  description = "HealthApp Database Password"

  tags = merge(local.common_tags, {
    Name = "healthapp-db-password"
  })
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = var.db_password # ✅ Matches DB password
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name        = "healthapp/jwt-secret"
  description = "HealthApp JWT Secret"

  tags = merge(local.common_tags, {
    Name = "healthapp-jwt-secret"
  })
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret != null ? var.jwt_secret : "default-jwt-secret-change-in-production"
}

# =========================================
# ✅ ECR, ECS, ALB, CLOUDWATCH (unchanged)
# =========================================
resource "aws_ecr_repository" "healthapp" {
  name                 = "healthapp"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration { scan_on_push = true }

  tags = merge(local.common_tags, {
    Name = "healthapp"
  })
}

# ECR Lifecycle Policy - keep last 10 images, delete older ones
resource "aws_ecr_lifecycle_policy" "healthapp" {
  repository = aws_ecr_repository.healthapp.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

resource "aws_ecs_cluster" "healthapp_cluster" {
  name = "healthapp-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = merge(local.common_tags, {
    Name = "healthapp-cluster"
  })
}

# IAM Roles
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "ecsTaskExecutionRole"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(local.common_tags, {
    Name = "ecs-task-execution-role"
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_secrets" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/SecretsManagerReadWrite"
}

# IAM Task Role (for application code running in containers)
resource "aws_iam_role" "ecs_task_role" {
  name = "ecsTaskRole"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(local.common_tags, {
    Name = "ecs-task-role"
  })
}

# Task role can be extended with additional policies as needed
# For now, it's minimal - add CloudWatch logs, S3, etc. as needed

# Application Load Balancer
resource "aws_lb" "healthapp_alb" {
  name               = "healthapp-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = [aws_subnet.public_1a.id, aws_subnet.public_1b.id]

  enable_deletion_protection = var.environment == "production"

  tags = merge(local.common_tags, {
    Name = "healthapp-alb"
  })
}

resource "aws_lb_target_group" "healthapp_tg" {
  name        = "healthapp-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.healthapp_vpc.id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 3
    interval            = 60
    matcher             = "200"
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 30
    unhealthy_threshold = 5
  }

  tags = merge(local.common_tags, {
    Name = "healthapp-tg"
  })
}

resource "aws_lb_listener" "healthapp_listener" {
  load_balancer_arn = aws_lb.healthapp_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.healthapp_tg.arn
  }
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "healthapp_logs" {
  name              = "/ecs/healthapp"
  retention_in_days = 30

  tags = merge(local.common_tags, {
    Name = "healthapp-logs"
  })
}

# ECS Task Definition
resource "aws_ecs_task_definition" "healthapp_task" {
  family                   = "healthapp-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name  = "healthapp"
      image = "${aws_ecr_repository.healthapp.repository_url}:latest"
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]
      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = "aws"
        },
        {
          name  = "DB_HOST"
          value = aws_db_instance.healthapp_db.address
        },
        {
          name  = "DB_USERNAME"
          value = "admin"
        },
        {
          name  = "DB_NAME"
          value = "healthapp"
        },
        {
          name  = "DB_PORT"
          value = "3306"
        }
      ]
      secrets = [
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = aws_secretsmanager_secret.db_password.arn
        },
        {
          name      = "JWT_SECRET"
          valueFrom = aws_secretsmanager_secret.jwt_secret.arn
        }
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.healthapp_logs.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = merge(local.common_tags, {
    Name = "healthapp-task"
  })
}

# ECS Service
resource "aws_ecs_service" "healthapp_service" {
  name            = "healthapp-service"
  cluster         = aws_ecs_cluster.healthapp_cluster.id
  task_definition = aws_ecs_task_definition.healthapp_task.arn
  desired_count   = var.ecs_desired_count != null ? var.ecs_desired_count : (var.environment == "production" ? 2 : 1)
  launch_type     = "FARGATE"

  # Health check grace period - gives containers time to start up
  health_check_grace_period_seconds = 300

  # Deployment configuration
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 50
  force_new_deployment               = true

  network_configuration {
    subnets          = [aws_subnet.private_1a.id, aws_subnet.private_1b.id]
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.healthapp_tg.arn
    container_name   = "healthapp"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.healthapp_listener]

  tags = merge(local.common_tags, {
    Name = "healthapp-service"
  })
}