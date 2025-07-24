# HealthApp Backend

A comprehensive Spring Boot application for calorie tracking and fitness management with MySQL database integration.

## Features

- **User Management**: Complete CRUD operations for users with fitness profiles
- **Food Tracking**: Track daily food consumption with detailed nutritional information
- **Activity Tracking**: Monitor daily physical activities and calorie burn
- **Calorie Management**: Calculate daily calorie intake vs. burn with remaining calorie goals
- **Security**: JWT-based authentication and authorization
- **API Documentation**: Swagger/OpenAPI integration
- **Database**: MySQL with JPA/Hibernate
- **Monitoring**: Spring Boot Actuator for health checks and metrics

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security**
- **Spring Data JPA**
- **MySQL 8.0**
- **JWT Authentication**
- **Swagger/OpenAPI**
- **Maven**

## Prerequisites

- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

## Database Setup

1. Create a MySQL database:
```sql
CREATE DATABASE healthapp;
```

2. Update database configuration in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/healthapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Running the Application

1. **Clone the repository**:
```bash
git clone https://github.com/kvrapuru12/HealthApp.git
cd HealthApp
```

2. **Build the project**:
```bash
mvn clean install
```

3. **Run the application**:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### User Management
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Food Tracking
- `GET /api/food-entries` - Get all food entries
- `GET /api/food-entries/{id}` - Get food entry by ID
- `GET /api/food-entries/user/{userId}` - Get food entries by user
- `GET /api/food-entries/user/{userId}/date/{date}` - Get food entries by user and date
- `POST /api/food-entries` - Create new food entry
- `PUT /api/food-entries/{id}` - Update food entry
- `DELETE /api/food-entries/{id}` - Delete food entry
- `GET /api/food-entries/user/{userId}/calories/{date}` - Get total calories consumed
- `GET /api/food-entries/user/{userId}/remaining-calories/{date}` - Get remaining calories

### Activity Tracking
- `GET /api/activity-entries` - Get all activity entries
- `GET /api/activity-entries/{id}` - Get activity entry by ID
- `GET /api/activity-entries/user/{userId}` - Get activity entries by user
- `GET /api/activity-entries/user/{userId}/date/{date}` - Get activity entries by user and date
- `POST /api/activity-entries` - Create new activity entry
- `PUT /api/activity-entries/{id}` - Update activity entry
- `DELETE /api/activity-entries/{id}` - Delete activity entry
- `GET /api/activity-entries/user/{userId}/calories-burned/{date}` - Get total calories burned
- `GET /api/activity-entries/user/{userId}/duration/{date}` - Get total activity duration
- `GET /api/activity-entries/user/{userId}/steps/{date}` - Get total steps
- `POST /api/activity-entries/calculate-calories` - Calculate calories burned for activity

### Documentation
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- API Docs: `http://localhost:8080/api/api-docs`

### Health Check
- Actuator Health: `http://localhost:8080/api/actuator/health`

## Project Structure

```
src/
├── main/
│   ├── java/com/healthapp/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── entity/          # JPA entities (User, FoodEntry, ActivityEntry)
│   │   ├── repository/      # Data access layer
│   │   ├── service/         # Business logic
│   │   └── HealthAppApplication.java
│   └── resources/
│       └── application.properties
└── test/                    # Test classes
```

## Development

### Adding New Features

1. Create entity classes in `src/main/java/com/healthapp/entity/`
2. Create repository interfaces in `src/main/java/com/healthapp/repository/`
3. Create service classes in `src/main/java/com/healthapp/service/`
4. Create controller classes in `src/main/java/com/healthapp/controller/`

### Testing

Run tests with:
```bash
mvn test
```

## Security

The application uses Spring Security with JWT tokens. Currently configured for development with basic security settings.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

MIT License
