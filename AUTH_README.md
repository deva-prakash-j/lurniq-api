# Lurniq API Authentication System

This Spring Boot application implements JWT-based authentication with Google OAuth2 support.

## Features

- JWT-based authentication
- Google OAuth2 login
- User registration and login
- Token refresh mechanism
- Protected routes
- MySQL database integration

## API Endpoints

### Authentication Endpoints

#### Register User
```
POST /api/auth/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "password123"
}
```

#### Login User
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "password123"
}
```

#### Refresh Token
```
POST /api/auth/refresh-token
Authorization: Bearer <refresh_token>
```

### Protected Endpoints

#### Get User Profile
```
GET /api/user/profile
Authorization: Bearer <access_token>
```

#### Test Authentication
```
GET /api/user/test
Authorization: Bearer <access_token>
```

### OAuth2 Endpoints

#### Google OAuth2 Login
```
GET /oauth2/authorization/google
```

This will redirect to Google's OAuth2 consent screen.

## Google OAuth2 Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google+ API
4. Create OAuth2 credentials (Web application)
5. Add authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google`
6. Set environment variables:
   ```bash
   export GOOGLE_CLIENT_ID="your-google-client-id"
   export GOOGLE_CLIENT_SECRET="your-google-client-secret"
   ```

## Environment Variables

```bash
# Database Configuration
DB_URL=jdbc:mysql://your-mysql-host:3306/lurniq
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password

# JWT Configuration
JWT_SECRET=your-jwt-secret-key-minimum-32-characters
JWT_EXPIRATION=86400000  # 24 hours
JWT_REFRESH_EXPIRATION=604800000  # 7 days

# Google OAuth2 Configuration
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
```

## Running the Application

1. Build the application:
   ```bash
   ./gradlew build
   ```

2. Run the application:
   ```bash
   ./gradlew bootRun
   ```

3. The application will start on `http://localhost:8080`

## Database Schema

The application will automatically create the required tables using Hibernate DDL auto-update.

## Frontend Integration

For OAuth2 authentication, redirect users to:
```
http://localhost:8080/oauth2/authorization/google
```

After successful authentication, users will be redirected to:
```
http://localhost:3000/auth/callback?token=<jwt_token>&refreshToken=<refresh_token>
```

## Security Features

- Stateless JWT authentication
- CORS configuration for frontend integration
- Password encryption using BCrypt
- Token validation and refresh mechanism
- OAuth2 integration with Google

## Usage Examples

### Frontend JavaScript Example

```javascript
// Register user
const registerResponse = await fetch('/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com',
    password: 'password123'
  })
});

const { accessToken, refreshToken } = await registerResponse.json();

// Make authenticated requests
const profileResponse = await fetch('/api/user/profile', {
  headers: { 'Authorization': `Bearer ${accessToken}` }
});
```

## Error Handling

The API returns appropriate HTTP status codes:
- 200: Success
- 400: Bad Request (validation errors)
- 401: Unauthorized (invalid credentials)
- 403: Forbidden (insufficient permissions)
- 500: Internal Server Error
