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
- **Mood Tracking**: `/moods` (Create, Read, Update, Delete mood entries)
- **Water Tracking**: `/water` (Create, Read, Update, Delete water consumption entries)
- **Step Tracking**: `/steps` (Create, Read, Update, Delete step entries)
- **Sleep Tracking**: `/sleeps` (Create, Read, Update, Delete sleep entries)

## Features

- User management with fitness profiles
- Food tracking with nutritional details
- Activity tracking and calorie burn
- Mood tracking and emotional wellness
- Water consumption tracking (10-5000ml range)
- Step tracking for daily step counts
- Sleep tracking for sleep quality
- JWT authentication with role-based access
- API documentation with Swagger
- Automated database migrations with Flyway
- Rate limiting and security measures

## Development

```bash
# Build
mvn clean package

# Test
mvn test

# Run
mvn spring-boot:run
```

## Deployment

For AWS deployment, CI/CD setup, and infrastructure configuration, see **[DEPLOYMENT.md](DEPLOYMENT.md)**.

**Quick Overview:**
- Complete Terraform infrastructure setup
- Automated CI/CD pipeline via GitHub Actions
- Deploys automatically on push to main branch
- Includes monitoring, security, and scaling guidance

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
