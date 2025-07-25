# HealthApp

A Spring Boot application for calorie tracking and fitness management.

## Quick Start

```bash
# Clone and setup
git clone https://github.com/kvrapuru12/HealthApp.git
cd HealthApp

# Run locally
mvn spring-boot:run
```

## Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8.0+

## Configuration

The application uses default MySQL settings. Update `src/main/resources/application.properties` if needed:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
jwt.secret=your-secret-key
```

## API Endpoints

- **Base URL**: `http://localhost:8080/api`
- **Swagger UI**: `/swagger-ui.html`
- **Health Check**: `/actuator/health`
- **Users**: `/users`
- **Food Tracking**: `/food-entries`
- **Activity Tracking**: `/activity-entries`

## Features

- User management with fitness profiles
- Food tracking with nutritional details
- Activity tracking and calorie burn
- JWT authentication
- API documentation with Swagger
- Automated database migrations with Flyway

## Development

```bash
# Build
mvn clean package

# Test
mvn test

# Run
mvn spring-boot:run
```

## CI/CD Pipeline

### Automated Deployment

The application has a complete CI/CD pipeline that automatically:

1. **Tests** - Runs all unit tests
2. **Builds** - Creates JAR file and Docker image
3. **Deploys** - Pushes to ECR and updates ECS
4. **Migrates** - Runs database migrations automatically
5. **Tests** - Verifies deployment health

### Trigger Deployment

Simply push to the main branch:
```bash
git push origin main
```

### Setup Required

1. **AWS Infrastructure**: Follow `AWS_DEPLOYMENT.md`
2. **GitHub Secrets**: Follow `GITHUB_SECRETS_SETUP.md`

## AWS Deployment

For production deployment to AWS:

1. **Setup AWS Infrastructure**: Follow `AWS_DEPLOYMENT.md`
2. **Configure GitHub Secrets**: Follow `GITHUB_SECRETS_SETUP.md`
3. **Deploy**: Push to main branch or run `./deploy-aws.sh`

**AWS Architecture**:
- ECS Fargate (containerized)
- RDS MySQL (database)
- Application Load Balancer
- CloudWatch (monitoring)
- Flyway (database migrations)

## Project Structure

```
src/main/java/com/healthapp/
├── config/          # Security & configuration
├── controller/      # REST controllers
├── entity/          # JPA entities
├── repository/      # Data access layer
├── service/         # Business logic
└── HealthAppApplication.java

src/main/resources/
├── db/migration/    # Database migration scripts
├── application.properties
└── application-aws.properties
```
# Infrastructure deployed successfully! Ready for first application deployment.
# Ready for deployment - Fri Jul 25 16:55:37 BST 2025
# Triggering deployment - Fri Jul 25 17:04:16 BST 2025
