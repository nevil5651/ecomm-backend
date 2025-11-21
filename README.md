# E-commerce Platform - Authentication Module

This module handles user authentication for the e-commerce platform, including registration, login, email verification, password reset, and OAuth2 login and rest of app is being builded.

## Features

- User Registration with Email Verification
- User Login with JWT
- Refresh Token with Rotation
- OAuth2 Login (Google, Facebook, etc.)
- Password Reset via Email
- Role-based Authorization (CUSTOMER, VENDOR, ADMIN, STAFF)

## Technology Stack

- Spring Boot
- Spring Security
- JWT (JSON Web Tokens)
- Redis
- Spring Data JPA
- Spring Mail (with Thymeleaf)
- MySQL (or any relational database)

## Setup

1. Clone the repository.
2. Configure the database in `application.yml` with help of `applicationexample.yml` file
3. Run the application.

## API Endpoints

- `POST /api/v1/auth/register` - Register a new user
- `GET /api/v1/auth/verify-email` - Verify email using token
- `POST /api/v1/auth/login` - Login user
- `POST /api/v1/auth/refresh-token` - Refresh access token
- `POST /api/v1/auth/logout` - Logout user
- `POST /api/v1/auth/forgot-password` - Request password reset
- `POST /api/v1/auth/reset-password` - Reset password

## OAuth2 Login

The module supports OAuth2 login. Currently, it is designed to handle multiple providers. The OAuth account details are stored in the `auth_oauth_accounts` table.

## Security

- JWT tokens are stored in HTTP-only cookies for access token and refresh token is returned in the body (for refresh token endpoint).
- Passwords are encoded using BCrypt.
- Refresh tokens are stored in Redis and are rotated on every use to prevent replay attacks.

## Exception Handling

Custom exceptions are thrown for various error conditions and are handled by a global exception handler which returns structured JSON responses.

## Contributing

Please read the contributing guidelines before submitting pull requests.

## License

This project is just for learning purpose
