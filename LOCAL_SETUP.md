# Local Development Setup

Quick guide to run HealthApp locally.

## 🚀 Quick Start

### 1. Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.0+

### 2. Database Setup
```sql
CREATE DATABASE healthapp;
```

### 3. Environment Configuration
Create a `.env` file in the project root:
```properties
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=your-secret-key-here-make-it-long-and-secure
JWT_EXPIRATION=86400000
```

### 4. Run Application
```bash
# Build and run
mvn spring-boot:run

# Or build first, then run
mvn clean package
java -jar target/healthapp-backend-1.0.0.jar
```

## 🌐 Access Points

- **Application**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **Health Check**: http://localhost:8080/api/actuator/health

## 🧪 Testing

```bash
# Run tests
mvn test

# Test specific endpoints
curl http://localhost:8080/api/users
curl http://localhost:8080/api/actuator/health
```

## 🔧 Troubleshooting

### Database Connection Issues
- Verify MySQL is running
- Check credentials in `.env` file
- Ensure database `healthapp` exists

### Port Already in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

### Build Issues
```bash
# Clean and rebuild
mvn clean install
```

## 📝 Notes

- Application uses `application.properties` for local configuration
- Database will be created automatically if it doesn't exist
- JWT secret should be at least 32 characters for security 