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

---

## 📝 Usage Examples

### Complete User Registration Flow

1. **Register User:**

```bash
curl -X POST http://localhost:8080/auth/create-user \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "userName": "johndoe",
    "email": "john@example.com",
    "password": "securePassword123"
  }'
```

2. **Login:**

```bash
curl -X POST http://localhost:8080/auth/user-varification/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "securePassword123"
  }'
```

3. **Send Email Verification:**

```bash
curl -X POST http://localhost:8080/auth/user-varification/send \
  -H "Authorization: Bearer <access-token>"
```

4. **Verify Email:**

```bash
curl -X POST "http://localhost:8080/auth/user-varification/verify?otp=123456" \
  -H "Authorization: Bearer <access-token>"
```

### Task Management Flow

1. **Create Task:**

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "taskTitle": "Complete Project",
    "taskDesc": "Finish the task management system",
    "date": "2025-08-15T14:00:00",
    "taskStatus": "TODO"
  }'
```

2. **Set Reminder:**

```bash
curl -X POST "http://localhost:8080/tasks/set-reminder?taskId=1&reminderDateTime=2025-08-14T09:00:00" \
  -H "Authorization: Bearer <access-token>"
```

3. **Update Task Status:**

```bash
curl -X PUT http://localhost:8080/tasks/1 \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "taskTitle": "Complete Project",
    "taskDesc": "Finish the task management system",
    "date": "2025-08-15T14:00:00",
    "taskStatus": "DONE"
  }'
```

---

## 🔗 Additional Resources

- **Swagger UI**: Available at `http://localhost:8080/swagger-ui.html` when the application is running
- **OpenAPI Specification**: Available at `http://localhost:8080/v3/api-docs`
- **Project Repository**: [GitHub Repository](https://github.com/Arkadipta-Kundu/opus)

---

_Last updated: August 10, 2025_
