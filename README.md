# Opus - Task Management System

A comprehensive task management REST API built with Spring Boot that provides user authentication, task management, email notifications, and reminder scheduling.

## 🚀 Features

### Core Features

- **User Management**: User registration, login, and profile management
- **Task Management**: Create, read, update, and delete tasks with status tracking
- **Authentication & Authorization**:
  - JWT-based authentication
  - OAuth2 integration with Google
  - Role-based access control
- **Email Services**:
  - Email verification for new users
  - Task reminder notifications
  - Password reset functionality
- **Task Reminders**: Scheduled email reminders for tasks
- **Caching**: Redis integration for improved performance
- **API Documentation**: Swagger/OpenAPI integration

### Task Status Management

Tasks support three status levels:

- `TODO` - Newly created tasks
- `IN_PROGRESS` - Tasks currently being worked on
- `DONE` - Completed tasks

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.5.4
- **Language**: Java 21
- **Database**: PostgreSQL
- **Caching**: Redis
- **Security**: Spring Security with JWT
- **Email**: Spring Mail with Gmail SMTP
- **Documentation**: SpringDoc OpenAPI
- **Build Tool**: Maven
- **Other Dependencies**:
  - Lombok for boilerplate code reduction
  - Jackson for JSON processing
  - Spring Data JPA for database operations
  - Spring Validation for input validation

## 📋 Prerequisites

Before running this application, make sure you have the following installed:

- Java 21 or higher
- Maven 3.6+
- PostgreSQL 12+
- Redis Server
- Git

## ⚙️ Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/Arkadipta-Kundu/opus.git
cd opus
```

### 2. Database Setup

#### PostgreSQL Setup

1. Install PostgreSQL and create a database named `opus`
2. Create a user with username `postgres` and password `0000` (or update `application.properties`)
3. Ensure PostgreSQL is running on localhost:5432

#### Redis Setup

1. Install and start Redis server
2. Default configuration runs on localhost:6379 (no password required)

### 3. Email Configuration

Update the email configuration in `src/main/resources/application.properties`:

```properties
# Gmail SMTP configuration
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

**Note**: For Gmail, you need to use an App Password instead of your regular password.

### 4. OAuth2 Google Setup (Optional)

To enable Google OAuth2 login:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Google+ API
4. Create OAuth2 credentials
5. Update the configuration in `application.properties`:

```properties
spring.security.oauth2.client.registration.google.client-id=your-client-id
spring.security.oauth2.client.registration.google.client-secret=your-client-secret
```

### 5. Configuration

Copy the template configuration file and update it with your settings:

```bash
cp src/main/resources/application.properties.template src/main/resources/application.properties
```

Update the following properties as needed:

- Database connection details
- Email configuration
- Redis configuration
- JWT secret key
- OAuth2 credentials

### 6. Build and Run

```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

Once the application is running, you can access:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## 🔐 Authentication

The API supports multiple authentication methods:

### 1. JWT Authentication

- Register a new user via `/auth/register`
- Login via `/auth/login` to receive JWT tokens
- Include JWT token in Authorization header: `Bearer <token>`

### 2. OAuth2 Google Login

- Access `/oauth2/authorization/google` to initiate Google login
- Users will be redirected to Google for authentication
- Upon successful authentication, JWT tokens are provided

### 3. Refresh Tokens

- Use refresh tokens to obtain new access tokens
- Endpoint: `/auth/refresh`

## 📡 Main API Endpoints

### Authentication Endpoints

- `POST /auth/register` - Register a new user
- `POST /auth/login` - User login
- `POST /auth/refresh` - Refresh JWT token
- `POST /auth/verify-email` - Verify email with OTP
- `POST /auth/forgot-password` - Request password reset
- `POST /auth/reset-password` - Reset password with OTP

### Task Management Endpoints

- `GET /api/tasks` - Get all tasks for authenticated user
- `POST /api/tasks` - Create a new task
- `GET /api/tasks/{id}` - Get task by ID
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task
- `POST /api/tasks/{id}/reminder` - Set reminder for a task

### User Management Endpoints

- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile
- `GET /admin/users` - Get all users (Admin only)

### Public Endpoints

- `GET /public/quotes` - Get random quotes
- `GET /public/health` - Health check

## 🔧 Configuration Properties

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/opus
spring.datasource.username=postgres
spring.datasource.password=0000

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT
jwt.secret=your-secret-key
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# Email
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/org/arkadipta/opus/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA entities
│   │   ├── repository/     # Data repositories
│   │   ├── security/       # Security configurations
│   │   ├── service/        # Business logic services
│   │   └── util/           # Utility classes
│   └── resources/
│       └── application.properties
└── test/                   # Test files
```

## 🧪 Testing

Run the tests using Maven:

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report
```

## 📋 Environment Variables

For production deployment, consider using environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/opus
export DB_USERNAME=postgres
export DB_PASSWORD=your-password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=your-jwt-secret
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
```

## 🚀 Deployment

### Docker Deployment (Recommended)

Create a `Dockerfile`:

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/opus-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Build and run:

```bash
mvn clean package
docker build -t opus-app .
docker run -p 8080:8080 opus-app
```

### Traditional Deployment

1. Build the JAR file: `mvn clean package`
2. Run the JAR: `java -jar target/opus-0.0.1-SNAPSHOT.jar`

## 🔍 Monitoring & Health Checks

The application includes:

- Health check endpoint: `/public/health`
- Application metrics via Spring Boot Actuator (if enabled)
- Logging configuration for debugging

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:

- Create an issue in the GitHub repository
- Contact: [arkadipta.dev@gmail.com](mailto:your-email@example.com)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- All contributors and users of this project
- Open source community for the various libraries used

---

**Note**: Make sure to update sensitive information like passwords, API keys, and email addresses before deploying to production.
