terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.67.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC and Networking
resource "aws_vpc" "healthapp_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "healthapp-vpc"
  }
}

resource "aws_subnet" "public_1a" {
  vpc_id                  = aws_vpc.healthapp_vpc.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "healthapp-public-1a"
  }
}

resource "aws_subnet" "public_1b" {
  vpc_id                  = aws_vpc.healthapp_vpc.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.aws_region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "healthapp-public-1b"
  }
}

resource "aws_subnet" "private_1a" {
  vpc_id            = aws_vpc.healthapp_vpc.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = "${var.aws_region}a"

  tags = {
    Name = "healthapp-private-1a"
  }
}

resource "aws_subnet" "private_1b" {
  vpc_id            = aws_vpc.healthapp_vpc.id
  cidr_block        = "10.0.4.0/24"
  availability_zone = "${var.aws_region}b"

  tags = {
    Name = "healthapp-private-1b"
  }
}

resource "aws_internet_gateway" "healthapp_igw" {
  vpc_id = aws_vpc.healthapp_vpc.id

  tags = {
    Name = "healthapp-igw"
  }
}

resource "aws_nat_gateway" "healthapp_nat" {
  allocation_id = aws_eip.nat_eip.id
  subnet_id     = aws_subnet.public_1a.id

  tags = {
    Name = "healthapp-nat"
  }

  depends_on = [aws_internet_gateway.healthapp_igw]
}

resource "aws_eip" "nat_eip" {
  tags = {
    Name = "healthapp-nat-eip"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.healthapp_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.healthapp_igw.id
  }

  tags = {
    Name = "healthapp-public-rt"
  }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.healthapp_vpc.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.healthapp_nat.id
  }

  tags = {
    Name = "healthapp-private-rt"
  }
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

# Security Groups
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

  tags = {
    Name = "healthapp-alb-sg"
  }
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

  tags = {
    Name = "healthapp-ecs-sg"
  }
}

resource "aws_security_group" "rds_sg" {
  name        = "healthapp-rds-sg"
  description = "Security group for RDS"
  vpc_id      = aws_vpc.healthapp_vpc.id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_sg.id]
  }

  tags = {
    Name = "healthapp-rds-sg"
  }
}

# RDS Database
resource "aws_db_subnet_group" "healthapp_db_subnet" {
  name       = "healthapp-db-subnet"
  subnet_ids = [aws_subnet.private_1a.id, aws_subnet.private_1b.id]

  tags = {
    Name = "healthapp-db-subnet"
  }
}

resource "aws_db_instance" "healthapp_db" {
  identifier           = "healthapp-db"
  engine               = "mysql"
  engine_version       = "8.0.35"
  instance_class       = "db.t3.micro"
  allocated_storage    = 20
  storage_type         = "gp2"
  storage_encrypted    = true
  username             = "admin"
  password             = var.db_password
  db_name              = "healthapp"
  skip_final_snapshot  = true
  db_subnet_group_name = aws_db_subnet_group.healthapp_db_subnet.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]

  tags = {
    Name = "healthapp-db"
  }
}

# ECR Repository
resource "aws_ecr_repository" "healthapp" {
  name                 = "healthapp"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "healthapp"
  }
}

# ECS Cluster
resource "aws_ecs_cluster" "healthapp_cluster" {
  name = "healthapp-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name = "healthapp-cluster"
  }
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
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_secrets" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/SecretsManagerReadWrite"
}

# Secrets Manager
resource "aws_secretsmanager_secret" "db_password" {
  name        = "healthapp/db-password"
  description = "HealthApp Database Password"
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = "HealthApp2024!SecurePassword123"
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name        = "healthapp/jwt-secret"
  description = "HealthApp JWT Secret"
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

# Application Load Balancer
resource "aws_lb" "healthapp_alb" {
  name               = "healthapp-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = [aws_subnet.public_1a.id, aws_subnet.public_1b.id]

  enable_deletion_protection = false

  tags = {
    Name = "healthapp-alb"
  }
}

resource "aws_lb_target_group" "healthapp_tg" {
  name        = "healthapp-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.healthapp_vpc.id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/api/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 2
  }

  tags = {
    Name = "healthapp-tg"
  }
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

  tags = {
    Name = "healthapp-logs"
  }
}

# ECS Task Definition
resource "aws_ecs_task_definition" "healthapp_task" {
  family                   = "healthapp-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

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

  tags = {
    Name = "healthapp-task"
  }
}

# ECS Service
resource "aws_ecs_service" "healthapp_service" {
  name            = "healthapp-service"
  cluster         = aws_ecs_cluster.healthapp_cluster.id
  task_definition = aws_ecs_task_definition.healthapp_task.arn
  desired_count   = 2
  launch_type     = "FARGATE"

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

  tags = {
    Name = "healthapp-service"
  }
} 