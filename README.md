# Spring Boot OAuth2 Authorization and Resource Server

## Project Overview

This project demonstrates a combined OAuth2 Authorization Server and Resource Server implementation using Spring Boot 3.4.4. It showcases how to secure RESTful APIs with OAuth2, enabling client credential flows with token-based authentication.

## Key Components

### 1. Core Application Structure
- **SimpleapiApplication**: Standard Spring Boot entry point
- **HomeController**: REST API with GET/POST/PUT endpoints for demonstration
- **SecurityConfiguration**: Combined authorization and resource server security setup

### 2. Security Implementation
The project implements a dual-purpose server that acts as both:
- **Authorization Server**: Issues OAuth2 tokens for authenticated clients
- **Resource Server**: Validates tokens and protects API resources

### 3. OAuth2 Configuration
- Multiple client configurations with different credentials
- Token time-to-live settings (5 minutes for access tokens, 60 minutes for refresh tokens)
- Support for client_credentials and refresh_token grant types
- Configured scopes (read, write)

### 4. API Documentation
- SpringDoc OpenAPI integration for Swagger UI documentation
- Endpoints publicly accessible without authentication

### 5. Docker Support
- Multi-stage Docker build for optimized container size
- First stage builds the application
- Second stage creates a minimal runtime image

## Technical Architecture

```mermaid
sequenceDiagram
    participant Client
    participant AuthServer as OAuth2 Authorization Server
    participant SecurityFilter as Security Filter Chain
    participant ResourceServer as Protected API Resources
    
    %% Token Request Flow
    Client->>AuthServer: POST /oauth2/token (client_credentials)
    Note over Client,AuthServer: Basic Auth with client_id:client_secret
    AuthServer->>AuthServer: Validate client credentials
    AuthServer->>Client: Access Token + Refresh Token
    
    %% API Access Flow
    Client->>SecurityFilter: Request /api/home (with Bearer token)
    SecurityFilter->>SecurityFilter: JWT validation
    SecurityFilter->>ResourceServer: Forward authenticated request
    ResourceServer->>Client: Protected resource response
    
    %% Token Refresh Flow
    Client->>AuthServer: POST /oauth2/token (refresh_token)
    AuthServer->>AuthServer: Validate refresh token
    AuthServer->>Client: New Access Token + Refresh Token
```

## Implementation Details

### Authorization Server Configuration
The project leverages Spring Security 6's built-in OAuth2 Authorization Server capabilities:

```java
@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", /* ... */)
                        .permitAll()
                        .anyRequest().authenticated())
                .with(authorizationServer(), Customizer.withDefaults())
                .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()))
                .csrf(CsrfConfigurer::disable)
                .cors(Customizer.withDefaults())
                .build();
    }
}
```

This configuration combines both roles:
1. **Authorization Server**: `.with(authorizationServer(), Customizer.withDefaults())`
2. **Resource Server**: `.oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()))`

### Client Configuration
Multiple clients are configured in `application.properties`:

```properties
# Main client
spring.security.oauth2.authorizationserver.client.oidc-client.registration.client-id=mcp-client
spring.security.oauth2.authorizationserver.client.oidc-client.registration.client-secret={noop}secret

# Additional clients
spring.security.oauth2.authorizationserver.client.customer1-client.registration.client-id=customer1
spring.security.oauth2.authorizationserver.client.customer1-client.registration.client-secret={noop}customer1
```

### Token Settings
```properties
spring.security.oauth2.authorizationserver.token.access-token-time-to-live=5m
spring.security.oauth2.authorizationserver.token.refresh-token-time-to-live=60m
```

## How to Use

### Obtaining an Access Token
```bash
curl -X POST 'http://localhost:8080/oauth2/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'Authorization: Basic bWNwLWNsaWVudDpzZWNyZXQ=' \
  -d 'grant_type=client_credentials&scope=read'
```

### Accessing Protected Resources
```bash
curl -X GET 'http://localhost:8080/api/home' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

### Running with Docker
```bash
# Build the Docker image
docker build -t maxiplux/simpleapi-authorization-and-resource-server:1.0.0 .

# Run the container
docker run -p 8080:8080 maxiplux/simpleapi-authorization-and-resource-server:1.0.0
```

## Technical Benefits

1. **Combined Server**: Eliminates the need for separate authorization and resource server services
2. **Standard OAuth2 Flow**: Implements industry-standard security patterns
3. **Token-Based Authentication**: Stateless authentication with configurable lifetimes
4. **Multiple Client Support**: Can handle various client applications with different credentials
5. **Docker Ready**: Easily deployable within containerized environments

## Prerequisites

- Java 17+
- Gradle 8.x
- Docker (optional)

## Built With

- [Spring Boot 3.4.4](https://spring.io/projects/spring-boot)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [SpringDoc OpenAPI UI](https://springdoc.org/)

## License

This project is available under the MIT License.
