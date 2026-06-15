variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]+$", var.aws_region))
    error_message = "AWS region must be a valid region identifier."
  }
}

variable "db_password" {
  description = "Database password (must be at least 8 characters)"
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.db_password) >= 8
    error_message = "Database password must be at least 8 characters long."
  }
}

variable "jwt_secret" {
  description = "JWT secret key (required, at least 32 characters)"
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.jwt_secret) >= 32
    error_message = "jwt_secret must be at least 32 characters."
  }
}

variable "environment" {
  description = "Environment name (production, staging, development)"
  type        = string
  default     = "production"

  validation {
    condition     = contains(["production", "staging", "development"], var.environment)
    error_message = "Environment must be one of: production, staging, development."
  }
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "healthapp"

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.app_name))
    error_message = "App name must contain only lowercase letters, numbers, and hyphens."
  }
}

variable "ecs_desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
  default     = null # Will use environment-based default if not specified
}

variable "enable_rds_backups" {
  description = "Enable automated backups for RDS"
  type        = bool
  default     = true
}

variable "rds_backup_retention_days" {
  description = "Number of days to retain RDS backups"
  type        = number
  default     = 7
}

variable "apple_client_id" {
  description = "Apple Sign In fallback client ID (APPLE_CLIENT_ID)"
  type        = string
  default     = ""
}

variable "apple_client_id_ios" {
  description = "Apple Sign In iOS client ID / Bundle ID (APPLE_CLIENT_ID_IOS)"
  type        = string
  default     = ""
}

variable "apple_client_id_android" {
  description = "Apple Sign In Android Services ID (APPLE_CLIENT_ID_ANDROID)"
  type        = string
  default     = ""
}

variable "apple_client_id_web" {
  description = "Apple Sign In Web Services ID (APPLE_CLIENT_ID_WEB)"
  type        = string
  default     = ""
}

variable "usda_api_key" {
  description = "USDA FoodData Central API key (stored in SSM as /healthapp/usda-api-key; optional)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "openai_api_key" {
  description = "OpenAI API key (stored in SSM as /healthapp/openai-api-key)"
  type        = string
  sensitive   = true
  default     = ""
}