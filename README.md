# HealthApp Backend

A Spring Boot application for calorie tracking and fitness management with MySQL database integration.

## 🚀 Quick Start

### Local Development
```bash
# Clone and setup
git clone https://github.com/kvrapuru12/HealthApp.git
cd HealthApp

# Create environment file
cp .env.example .env
# Edit .env with your database credentials

# Run locally
mvn spring-boot:run
```

## 📋 Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+**

## 🏗️ Technology Stack

- **Spring Boot 3.2.0**
- **Spring Security** with JWT
- **Spring Data JPA** with Hibernate
- **MySQL 8.0**
- **Swagger/OpenAPI**
- **Maven**

## 📁 Project Structure

```
src/
├── main/java/com/healthapp/
│   ├── config/          # Security & configuration
│   ├── controller/      # REST controllers
│   ├── entity/          # JPA entities
│   ├── repository/      # Data access layer
│   ├── service/         # Business logic
│   └── HealthAppApplication.java
└── resources/
    └── application.properties      # Local configuration
```

## 🔧 Configuration

### Local Development
Create `.env` file:
```properties
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000
```

## 🌐 API Endpoints

### Base URL
- **Local**: `http://localhost:8080/api`

### Key Endpoints
- **Swagger UI**: `/swagger-ui.html`
- **Health Check**: `/actuator/health`
- **Users**: `/users`
- **Food Tracking**: `/food-entries`
- **Activity Tracking**: `/activity-entries`

## 🚀 Deployment

### Local Development
```bash
mvn spring-boot:run
```

## 📊 Features

- **User Management**: CRUD operations with fitness profiles
- **Food Tracking**: Daily calorie intake with nutritional details
- **Activity Tracking**: Physical activities and calorie burn
- **Calorie Management**: Daily goals and remaining calories
- **Security**: JWT authentication
- **API Documentation**: Swagger/OpenAPI
- **Monitoring**: Health checks and metrics

## 🔐 Security

- JWT-based authentication
- Secure password storage
- SSL/TLS encryption
- CORS configuration

## 📈 Monitoring

- **Health Checks**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Logging**: Structured logging with different levels

## 🛠️ Development

### Building
```bash
mvn clean package
```

### Testing
```bash
mvn test
```

### Adding Features
1. Create entity in `entity/` package
2. Create repository in `repository/` package
3. Create service in `service/` package
4. Create controller in `controller/` package

## 📚 Documentation

- **API Docs**: Swagger UI at `/swagger-ui.html`
- **Local Setup**: See Quick Start section

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📞 Support

- **Issues**: Create GitHub issue
- **Local**: Check troubleshooting section

---

**🎉 Ready to deploy!** Follow the local setup guide to get started.
