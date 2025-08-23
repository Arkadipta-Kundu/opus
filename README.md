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

## 📡 API DOC

# Opus API Documentation

This document provides comprehensive documentation for the Opus Task Management System REST API.

## 🌐 Base URL

```
http://localhost:8080
```

## 🔐 Authentication

Most endpoints require authentication. The API supports JWT-based authentication. Include the JWT token in the `Authorization` header:

```
Authorization: Bearer <your-jwt-token>
```

## 📚 API Endpoints Overview

### Authentication Endpoints

- **User Registration**: `POST /auth/create-user`
- **User Login**: `POST /auth/user-varification/login`
- **Email Verification**: `POST /auth/user-varification/send` & `POST /auth/user-varification/verify`
- **Password Reset**: `POST /auth/user-varification/forget-password` & `POST /auth/user-varification/reset-password`
- **Token Refresh**: `POST /auth/user-varification/refresh`

### Task Management Endpoints

- **Get All Tasks**: `GET /tasks`
- **Create Task**: `POST /tasks`
- **Get Task by ID**: `GET /tasks/{id}`
- **Update Task**: `PUT /tasks/{id}`
- **Delete Task**: `DELETE /tasks/{id}`
- **Set Task Reminder**: `POST /tasks/set-reminder`
- **Remove Task Reminder**: `DELETE /tasks/reminder/{taskId}`
- **Get Tasks with Reminders**: `GET /tasks/with-reminders`

### User Management Endpoints

- **Get User by ID**: `GET /user/{id}`
- **Update User**: `PUT /user/{id}`
- **Delete User**: `DELETE /user/{id}`

### Admin Endpoints

- **Create Admin**: `POST /admin/create-admin`
- **Get All Users**: `GET /admin/users`
- **Get User by ID**: `GET /admin/users/{id}`
- **Delete User**: `DELETE /admin/users/{id}`
- **Get All Tasks**: `GET /admin/tasks`

### Public Endpoints

- **Health Check**: `GET /public/health`

---

## 📋 Detailed API Reference

### 🔑 Authentication Endpoints

#### Create User (Register)

**POST** `/auth/create-user`

Register a new user account.

**Request Body:**

```json
{
  "name": "string",
  "userName": "string",
  "email": "string",
  "password": "string",
  "roles": ["USER"]
}
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "John Doe",
  "userName": "johndoe",
  "email": "john@example.com",
  "roles": ["USER"],
  "emailVerified": false
}
```

---

#### User Login

**POST** `/auth/user-varification/login`

Authenticate user and receive JWT tokens.

**Request Body:**

```json
{
  "username": "string",
  "password": "string",
  "rememberMe": false
}
```

**Response:** `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

#### Send Email Verification OTP

**POST** `/auth/user-varification/send`

Send OTP to user's email for verification.

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`

```json
"OTP sent to your email address"
```

---

#### Verify Email OTP

**POST** `/auth/user-varification/verify`

Verify the OTP sent to user's email.

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**

- `otp` (required): The OTP code received via email

**Response:** `200 OK`

```json
"Email verified successfully"
```

---

#### Check Email Verification Status

**GET** `/auth/user-varification/is-verified`

Check if the user's email is verified.

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`

```json
true
```

---

#### Forgot Password

**POST** `/auth/user-varification/forget-password`

Request password reset token.

**Request Body:**

```json
{
  "email": "user@example.com"
}
```

**Response:** `200 OK`

```json
"Password reset token sent to your email"
```

---

#### Reset Password

**POST** `/auth/user-varification/reset-password`

Reset password using the token received via email.

**Query Parameters:**

- `token` (required): Password reset token

**Request Body:**

```json
{
  "newPassword": "newSecurePassword123"
}
```

**Response:** `200 OK`

```json
"Password reset successfully"
```

---

#### Refresh Token

**POST** `/auth/user-varification/refresh`

Get new access token using refresh token.

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:** `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

### 📝 Task Management Endpoints

#### Get All Tasks for Logged-in User

**GET** `/tasks`

Retrieve all tasks belonging to the authenticated user.

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`

```json
[
  {
    "taskId": 1,
    "taskTitle": "Complete API Documentation",
    "taskDesc": "Write comprehensive API documentation for the project",
    "date": "2025-08-10T10:30:00",
    "taskStatus": "IN_PROGRESS",
    "reminderDateTime": "2025-08-11T09:00:00",
    "reminderEnabled": true,
    "reminderSent": false,
    "reminderEmail": "user@example.com"
  }
]
```

---

#### Create New Task

**POST** `/tasks`

Create a new task for the authenticated user.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```json
{
  "taskTitle": "New Task",
  "taskDesc": "Task description",
  "date": "2025-08-15T14:00:00",
  "taskStatus": "TODO"
}
```

**Response:** `200 OK`

```json
{
  "message": "Task created successfully",
  "taskId": 2
}
```

---

#### Get Task by ID

**GET** `/tasks/{id}`

Retrieve a specific task by its ID.

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**

- `id` (required): Task ID

**Response:** `200 OK`

```json
{
  "taskId": 1,
  "taskTitle": "Complete API Documentation",
  "taskDesc": "Write comprehensive API documentation for the project",
  "date": "2025-08-10T10:30:00",
  "taskStatus": "IN_PROGRESS",
  "reminderDateTime": "2025-08-11T09:00:00",
  "reminderEnabled": true,
  "reminderSent": false,
  "reminderEmail": "user@example.com"
}
```

---

#### Update Task

**PUT** `/tasks/{id}`

Update an existing task.

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**

- `id` (required): Task ID

**Request Body:**

```json
{
  "taskTitle": "Updated Task Title",
  "taskDesc": "Updated task description",
  "date": "2025-08-15T14:00:00",
  "taskStatus": "DONE"
}
```

**Response:** `200 OK`

```json
{
  "message": "Task updated successfully"
}
```

---

#### Delete Task

**DELETE** `/tasks/{id}`

Delete a specific task.

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**

- `id` (required): Task ID

**Response:** `200 OK`

```json
{
  "message": "Task deleted successfully"
}
```

---

#### Set Task Reminder

**POST** `/tasks/set-reminder`

Set a reminder for a specific task.

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**

- `taskId` (required): Task ID
- `reminderDateTime` (required): Reminder date and time (ISO format)
- `customEmail` (optional): Custom email for reminder (defaults to user's email)

**Example:**

```
POST /tasks/set-reminder?taskId=1&reminderDateTime=2025-08-11T09:00:00&customEmail=custom@example.com
```

**Response:** `200 OK`

```json
{
  "message": "Reminder set successfully"
}
```

---

#### Remove Task Reminder

**DELETE** `/tasks/reminder/{taskId}`

Remove reminder from a specific task.

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**

- `taskId` (required): Task ID

**Response:** `200 OK`

```json
{
  "message": "Reminder removed successfully"
}
```

---

#### Get Tasks with Reminders

**GET** `/tasks/with-reminders`

Retrieve all tasks that have reminders set.

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`

```json
[
  {
    "taskId": 1,
    "taskTitle": "Complete API Documentation",
    "taskDesc": "Write comprehensive API documentation for the project",
    "date": "2025-08-10T10:30:00",
    "taskStatus": "IN_PROGRESS",
    "reminderDateTime": "2025-08-11T09:00:00",
    "reminderEnabled": true,
    "reminderSent": false,
    "reminderEmail": "user@example.com"
  }
]
```

---

### 👤 User Management Endpoints

#### Get User by ID

**GET** `/user/{id}`

Get user details by ID.

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**

- `id` (required): User ID

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "John Doe",
  "userName": "johndoe",
  "email": "john@example.com",
  "roles": ["USER"],
  "emailVerified": true
}
```

---

#### Update User

**PUT** `/user/{id}`

Update user information.

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**

- `id` (required): User ID

**Request Body:**

```json
{
  "name": "Updated Name",
  "userName": "updatedusername",
  "email": "newemail@example.com"
}
```

**Response:** `200 OK`

```json
{
  "message": "User updated successfully"
}
```

---

#### Delete User

**DELETE** `/user/{id}`

Delete user account.

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**

- `id` (required): User ID

**Response:** `200 OK`

```json
{
  "message": "User deleted successfully"
}
```

---

### 🔧 Admin Endpoints

#### Create Admin

**POST** `/admin/create-admin`

Create a new admin user. (Admin access required)

**Headers:** `Authorization: Bearer <admin-token>`

**Request Body:**

```json
{
  "name": "Admin User",
  "userName": "adminuser",
  "email": "admin@example.com",
  "password": "securePassword123",
  "roles": ["ADMIN"]
}
```

**Response:** `200 OK`

```json
{
  "message": "Admin created successfully"
}
```

---

#### Get All Users (Admin)

**GET** `/admin/users`

Retrieve all users in the system. (Admin access required)

**Headers:** `Authorization: Bearer <admin-token>`

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "name": "John Doe",
    "userName": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"],
    "emailVerified": true
  },
  {
    "id": 2,
    "name": "Jane Smith",
    "userName": "janesmith",
    "email": "jane@example.com",
    "roles": ["USER"],
    "emailVerified": false
  }
]
```

---

#### Get User by ID (Admin)

**GET** `/admin/users/{id}`

Get specific user details. (Admin access required)

**Headers:** `Authorization: Bearer <admin-token>`

**Path Parameters:**

- `id` (required): User ID

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "John Doe",
  "userName": "johndoe",
  "email": "john@example.com",
  "roles": ["USER"],
  "emailVerified": true,
  "tasks": [...]
}
```

---

#### Delete User (Admin)

**DELETE** `/admin/users/{id}`

Delete any user account. (Admin access required)

**Headers:** `Authorization: Bearer <admin-token>`

**Path Parameters:**

- `id` (required): User ID

**Response:** `200 OK`

```json
{
  "message": "User deleted successfully"
}
```

---

#### Get All Tasks (Admin)

**GET** `/admin/tasks`

Retrieve all tasks in the system. (Admin access required)

**Headers:** `Authorization: Bearer <admin-token>`

**Response:** `200 OK`

```json
[
  {
    "taskId": 1,
    "taskTitle": "Complete API Documentation",
    "taskDesc": "Write comprehensive API documentation",
    "date": "2025-08-10T10:30:00",
    "taskStatus": "IN_PROGRESS",
    "user": {
      "id": 1,
      "userName": "johndoe"
    }
  }
]
```

---

### 🌍 Public Endpoints

#### Health Check

**GET** `/public/health`

Check the health status of the API.

**Response:** `200 OK`

```json
{
  "status": "UP",
  "timestamp": "2025-08-10T12:00:00Z",
  "version": "0.0.1-SNAPSHOT"
}
```

---

## 📊 Data Models

### User Model

```json
{
  "id": "integer (int64)",
  "name": "string",
  "userName": "string",
  "roles": ["string"],
  "email": "string",
  "password": "string",
  "tasks": ["Task"],
  "emailVerified": "boolean"
}
```

### Task Model

```json
{
  "taskId": "integer (int64)",
  "taskTitle": "string",
  "taskDesc": "string",
  "date": "string (date-time)",
  "taskStatus": "enum [TODO, IN_PROGRESS, DONE]",
  "user": "User",
  "reminderDateTime": "string (date-time)",
  "reminderEnabled": "boolean",
  "reminderSent": "boolean",
  "reminderEmail": "string"
}
```

### Login Request Model

```json
{
  "username": "string (required, min length: 1)",
  "password": "string (required, min length: 1)",
  "rememberMe": "boolean"
}
```

### Refresh Token Request Model

```json
{
  "refreshToken": "string"
}
```

---

## 🚨 Error Responses

### Common Error Codes

#### 400 Bad Request

```json
{
  "error": "Bad Request",
  "message": "Invalid input data",
  "timestamp": "2025-08-10T12:00:00Z"
}
```

#### 401 Unauthorized

```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "timestamp": "2025-08-10T12:00:00Z"
}
```

#### 403 Forbidden

```json
{
  "error": "Forbidden",
  "message": "Access denied",
  "timestamp": "2025-08-10T12:00:00Z"
}
```

#### 404 Not Found

```json
{
  "error": "Not Found",
  "message": "Resource not found",
  "timestamp": "2025-08-10T12:00:00Z"
}
```

#### 500 Internal Server Error

```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "timestamp": "2025-08-10T12:00:00Z"
}
```

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

### Azure Deployment (Recommended for Production) ☁️

For cloud deployment, we provide comprehensive Azure App Service deployment:

**Quick Start:**

```bash
# For Linux/Mac users
chmod +x deploy-to-azure.sh
./deploy-to-azure.sh

# For Windows users
deploy-to-azure.bat
```

**Manual Deployment:**
See the detailed [Azure Deployment Guide](AZURE_DEPLOYMENT_GUIDE.md) for:

- Step-by-step Azure App Service deployment
- Azure Container Instances deployment
- Environment configuration
- Security best practices
- Scaling and monitoring setup
- Cost optimization strategies

### Docker Deployment

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
