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

## Development

```bash
# Build
mvn clean package

# Test
mvn test

# Run
mvn spring-boot:run
```

## AWS Deployment

For production deployment to AWS:

1. **Setup AWS Infrastructure**: Follow `AWS_DEPLOYMENT.md`
2. **Deploy**: Run `./deploy-aws.sh`

**AWS Architecture**:
- ECS Fargate (containerized)
- RDS MySQL (database)
- Application Load Balancer
- CloudWatch (monitoring)

## Project Structure

```
src/main/java/com/healthapp/
├── config/          # Security & configuration
├── controller/      # REST controllers
├── entity/          # JPA entities
├── repository/      # Data access layer
├── service/         # Business logic
└── HealthAppApplication.java
```
