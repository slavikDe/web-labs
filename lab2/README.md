# Lab 2 - User Authentication and Authorization System

## Overview
This project implements a comprehensive user authentication and authorization system using Java servlets with role-based access control. The application is built as a multi-module Maven project with a layered architecture.

## Project Structure
```
lab2/
├── lab2.core/          # Core business logic and utilities
├── lab2.webapp/        # Web application layer (servlets, filters)
├── lab2.persistance/   # Data persistence layer
└── pom.xml            # Parent Maven configuration
```

## Requirements Implementation

### 1. Password Security
- При реєстрації та при оновленні користувача пароль зберігати в зашифрованому вигляді
- Passwords are encrypted before storage using secure hashing algorithms

### 2. Login Endpoint
- Створити запит /users/login на вхід
- Login endpoint available at `POST /users/login`

### 3. Token-Based Authorization
- Застосувати авторизацію: в запиті відправити authToken в заголовку Authorization
- Authorization tokens sent in `Authorization` header
- Server verifies tokens and authorizes users accordingly

### 4. Role-Based Access Control
- Забезпечити використання ролей (user та admin) з відповідними діями
- Support for `user` and `admin` roles with appropriate permissions

## Technical Stack
- **Java**: 21
- **Build Tool**: Maven
- **Web Framework**: Jakarta Servlets
- **Testing**: JUnit

## API Endpoints

### Authentication
- `POST /users/register` - User registration
- `POST /users/login` - User login
- `GET /users/*` - User information (requires authentication)

## Setup and Installation

### Prerequisites
- Java 21 or higher
- Maven 3.6+

### Building the Project
```bash
  mvn clean compile
```

### Running the Application
```bash
  mvn package
  # Deploy the generated WAR file to your servlet container

```


### Testing
```bash
  mvn test
  ```


## Security Features
- Password encryption for secure storage
- JWT token-based authentication
- Role-based authorization filters
- Secure session management

## Development Status
- ✅ Project structure setup
- ✅ Basic servlet endpoints
- 🔄 Authentication implementation (in progress)
- 🔄 Authorization filters (in progress)
- 🔄 Database integration (pending)
- 🔄 Password encryption (pending)
