# Spring Boot OAuth2 Authorization Server

This project implements a Spring Boot 3.4.4 OAuth2 Authorization Server with database persistence for client
credentials.

## Features

- Spring Boot 3.4.4 with Spring Security OAuth2 Authorization Server
- Database persistence for OAuth2 client details using JPA
- Token customization with additional claims (scopes, authorities, client profile)
- Support for standard OAuth2 flows (authorization code, client credentials)
- Secure password encoding for client secrets
- REST API for client management
- H2 in-memory database (configurable for production databases)

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle

### Running the Application

```bash
./gradlew bootRun
```

The application will start on port 8080 by default.

### Default Clients

The application is initialized with the following default clients:

1. **MCP Client**
    - Client ID: `mcp-client`
    - Client Secret: `secret`
    - Scopes: `read`, `write`
    - Grant Types: `client_credentials`, `refresh_token`

2. **Customer 1**
    - Client ID: `customer1`
    - Client Secret: `customer1`
    - Scopes: `read`, `write`
    - Grant Types: `client_credentials`, `refresh_token`

3. **Customer 2**
    - Client ID: `customer2`
    - Client Secret: `customer2`
    - Scopes: `read`, `write`
    - Grant Types: `client_credentials`, `refresh_token`

## API Endpoints

### OAuth2 Authorization Server Endpoints

- **Token Endpoint**: `/oauth2/token`
- **Authorization Endpoint**: `/oauth2/authorize`
- **JWK Set Endpoint**: `/oauth2/jwks`

### Client Management API

- **GET /api/clients**: Get all registered clients
- **GET /api/clients/{id}**: Get a client by ID
- **POST /api/clients**: Create a new client
- **PUT /api/clients/{id}**: Update an existing client
- **DELETE /api/clients/{id}**: Delete a client

## Obtaining Tokens

### Client Credentials Flow

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'mcp-client:secret' | base64)" \
  -d "grant_type=client_credentials&scope=read"
```

### Refresh Token Flow

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'mcp-client:secret' | base64)" \
  -d "grant_type=refresh_token&refresh_token=YOUR_REFRESH_TOKEN"
```

## Client Registration Example

```bash
curl -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "new-client",
    "clientSecret": "new-secret",
    "clientName": "New Client",
    "clientDescription": "A new client for testing",
    "clientEmail": "new@example.com",
    "clientWebsite": "https://example.com",
    "clientAuthenticationMethods": ["client_secret_basic"],
    "authorizationGrantTypes": ["client_credentials", "refresh_token"],
    "scopes": ["read", "write"],
    "authorities": ["ROLE_CLIENT"],
    "requireProofKey": false,
    "requireAuthorizationConsent": false,
    "accessTokenTimeToLiveSeconds": 300,
    "refreshTokenTimeToLiveSeconds": 3600
  }'
```

## Token Customization

The authorization server customizes tokens with additional claims:

- **client_name**: The name of the client
- **email**: The client's email address
- **website**: The client's website
- **description**: The client's description
- **authorities**: The client's authorities/roles
- **scopes**: The authorized scopes

## Configuration

The main configuration properties are in `application.properties`:

- **Database Configuration**: Configure the database connection
- **Token Settings**: Configure token validity periods
- **CORS Settings**: Configure CORS for cross-origin requests

## Security Considerations

- Client secrets are stored securely using BCrypt password encoding
- Client secrets are never returned in API responses (masked as `[PROTECTED]`)
- H2 console is secured and only accessible to authenticated users
- CORS is configured to restrict cross-origin requests

## Architecture

The application follows a layered architecture:

- **Controller Layer**: REST endpoints for client management
- **Service Layer**: Business logic and OAuth2 client registration
- **Repository Layer**: Data access using Spring Data JPA
- **Entity Layer**: JPA entities for database persistence

## License

This project is licensed under the MIT License - see the LICENSE file for details.