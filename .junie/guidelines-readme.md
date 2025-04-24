# Spring Boot 3.4 Best Practices Guidelines

## Table of Contents
1. [Introduction](#introduction)
2. [Architecture Style](#1-architecture-style)
   1. [Layered Architecture](#11-adopt-a-layered-architecture)
   2. [Service-Oriented Structure](#12-service-oriented-structure)
   3. [Configuration Management](#13-configuration-management)
   4. [Directory Structure](#14-directory-structure)
3. [Controllers](#2-controllers)
   1. [REST Controllers Best Practices](#21-rest-controllers-best-practices)
   2. [Request/Response Objects](#22-requestresponse-objects)
   3. [Exception Handling](#23-exception-handling)
4. [Testing](#3-testing)
   1. [Test Framework Requirements](#31-test-framework-requirements)
   2. [Working with MockBean](#32-working-with-mockbean)
   3. [Unit Testing vs Integration Testing](#33-unit-testing-with-mock-vs-integration-testing-with-mockitobean)
   4. [Testing Controller Layers](#34-testing-controller-layers-with-mockmvc-and-mockbean)
   5. [Testing with Slices](#35-testing-with-slices-and-mockbean)
5. [Libraries and Dependencies](#4-libraries-and-dependencies)
   1. [Core Libraries](#41-core-libraries)
   2. [Database](#42-database)
   3. [Logging](#43-logging)
   4. [API Documentation](#44-api-documentation)
   5. [HTTP Client with RestClient](#45-http-client-with-restclient)
6. [Common Pitfalls and Gotchas](#5-common-pitfalls-and-gotchas)
   1. [Frequent Mistakes to Avoid](#51-frequent-mistakes-to-avoid)
   2. [Edge Cases to Consider](#52-edge-cases-to-consider)
   3. [Version Compatibility](#53-version-compatibility)
   4. [Anti-Patterns to Avoid](#54-anti-patterns-to-avoid)
7. [Performance Optimization Techniques](#6-performance-optimization-techniques)
   1. [Database Query Optimization](#61-database-query-optimization)
   2. [Caching](#62-caching)
   3. [Asynchronous Processing](#63-asynchronous-processing)
   4. [Pagination](#64-pagination)
   5. [Load Testing](#65-load-testing)
8. [Development Environment and Tooling](#7-development-environment-and-tooling)
   1. [Recommended Tools](#71-recommended-tools)
   2. [Code Quality Tools](#72-code-quality-tools)
9. [General Best Practices](#8-general-best-practices)
   1. [Code Quality](#81-code-quality)
   2. [Performance Optimization](#82-performance-optimization)
   3. [Documentation](#83-documentation)
10. [Additional Considerations](#9-additional-considerations)
    1. [Internationalization and Localization](#91-internationalization-and-localization)
    2. [Advanced API Design Principles](#92-advanced-api-design-principles)

## Introduction

This document outlines the architecture, development, and testing guidelines for applications built with Spring Boot 3.4. Following these practices will help ensure scalable, maintainable, and robust applications.

## 1. Architecture Style

### 1.1 Adopt a Layered Architecture

Implement a clear layered architecture:

- **Presentation Layer**: Controllers, View Templates, REST endpoints
- **Service Layer**: Business logic, transaction management
- **Data Access Layer**: Repositories, data source configurations
- **Domain Layer**: Entity models, value objects

### 1.2 Service-Oriented Structure

- Group related functionality into cohesive services
- Design services around business capabilities
- Ensure loose coupling between services
- Implement proper error handling and retry mechanisms
- Use interface contracts for service definitions

### 1.3 Configuration Management

- Externalize configuration using Spring's `@ConfigurationProperties`
- Use profiles for environment-specific configurations
- Leverage Spring Cloud Config for centralized configuration in multi-service environments
- Sensitive configuration should be handled using Spring Boot's secret management or external vaults

### 1.4 Directory Structure

Follow a consistent directory structure to improve maintainability:

```
src/
 ├── main/
 │   ├── java/
 │   │   └── com/example/app/
 │   │       ├── Application.java (Main entry point)
 │   │       ├── config/          (Configuration classes)
 │   │       ├── controller/      (REST controllers)
 │   │       ├── service/         (Business logic services)
 │   │       ├── repository/      (Data access repositories)
 │   │       ├── model/           (Entities) 
 │   │       ├── model/contract/
 │   │       │   ├── response/    (DTOs for responses)
 │   │       │   ├── request/     (DTOs for requests)
 │   │       │   └── dto/         (DTOs for internal use)
 │   │       ├── exception/       (Custom exceptions)
 │   │       └── util/            (Utility classes)
 │   └── resources/
 │       ├── application.properties or application.yml
 │       ├── static/            (Static resources)
 │       └── templates/         (View templates)
 └── test/
     ├── java/
     │   └── com/example/app/
     │       ├── controller/      (Controller tests)
     │       ├── service/         (Service tests)
     │       └── repository/      (Repository tests)
     └── resources/
         └── application-test.properties or application-test.yml
```

- **Root Package**: Use a meaningful root package name (e.g., `com.company.appname`)
- **Modularization**: For larger applications, consider breaking down the application into modules based on business domains

## 2. Controllers

### 2.1 REST Controllers Best Practices

- Follow REST principles for resource naming and HTTP methods
- Use proper HTTP status codes for different scenarios
- Implement versioning strategy (URI path, request parameters, or headers)
- Keep controllers thin, delegate business logic to services
- Document APIs using OpenAPI (Springdoc)

```java
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        log.info("Retrieving user with id: {}", id);
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

### 2.2 Request/Response Objects

- Use dedicated DTOs for request and response objects
- Implement input validation using Bean Validation (JSR-380)
- Create specific response models instead of returning entity objects directly
- Use Jackson annotations for JSON customization when needed

```java
@Data
@Builder
public class UserCreationRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @Email(message = "Valid email is required")
    private String email;
    
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}

// Example of creating an object using the builder pattern
UserCreationRequest request = UserCreationRequest.builder()
    .name("John Doe")
    .email("john@example.com")
    .password("securePassword123")
    .build();
```

### 2.3 Exception Handling

- Implement a global exception handler using `@ControllerAdvice`
- Create custom exceptions for different error scenarios
- Return consistent error responses with appropriate HTTP status codes
- Include meaningful error messages that are safe to expose

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation failed: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        
        ErrorResponse errorResponse = new ErrorResponse("VALIDATION_FAILED", "Validation failed", errors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
```

## 3. Testing

### 3.1 Test Framework Requirements

- **JUnit 5**: All tests MUST use JUnit 5 (Jupiter) as the testing framework
- **Mockito**: Use Mockito for mocking dependencies
- **Spring Boot Test**: Leverage Spring Boot's testing utilities
- **AssertJ**: Preferred for fluent assertions

Follow the test pyramid approach:
- **Unit Tests**: Most numerous, test individual components in isolation
- **Integration Tests**: Test interactions between components
- **End-to-End Tests**: Fewer tests covering critical business flows

### 3.2 Working with MockBean

`@MockBean` is a Spring Boot test annotation that adds Mockito mocks to the Spring ApplicationContext. It's essential for proper Spring Boot integration testing:

```java
@SpringBootTest
class UserServiceIntegrationTest {
    
    // Replace real bean with a mock in Spring context
    @MockBean
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Test
    @DisplayName("Should return user when repository finds one")
    void shouldReturnUser_whenRepositoryFindsOne() {
        // Arrange
        User mockUser = User.builder()
            .id(1L)
            .name("Test User")
            .email("test@example.com")
            .build();
            
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        
        // Act
        Optional<UserDto> result = userService.findById(1L);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test User");
        
        // Verify mock interactions
        verify(userRepository).findById(1L);
    }
}
```

#### Key points when working with MockBean:

1. **Context Management**: `@MockBean` replaces or adds beans to the Spring context
2. **Slower Tests**: Using `@MockBean` causes the Spring context to reload, so use sparingly
3. **Reset After Tests**: Mocks are automatically reset after each test
4. **Bean Naming**: `@MockBean` can specify the name of the bean to replace with `name` attribute
5. **Verification**: Always verify important interactions with the mock

### 3.3 Unit Testing with @Mock vs Integration Testing with @MockBean

```java
// Unit Testing: Lighter weight, no Spring context
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock // Lightweight Mockito mock, no Spring context involved
    private UserRepository userRepository;
    
    @InjectMocks // Manually injects mocks into the class under test
    private UserServiceImpl userService;
    
    @Test
    @DisplayName("Should create user when given valid data")
    void shouldCreateUser_whenValidData() {
        // Test implementation
    }
}

// Integration Testing: Full Spring context
@SpringBootTest
class UserServiceIntegrationTest {
    
    @MockBean // Spring context aware, replaces the real bean
    private UserRepository userRepository;
    
    @Autowired // Injected by Spring with the mock
    private UserService userService;
    
    @Test
    @DisplayName("Should retrieve user when exists")
    void shouldRetrieveUser_whenExists() {
        // Test implementation
    }
}
```

### 3.4 Testing Controller Layers with MockMvc and MockBean

```java
@WebMvcTest(UserController.class) // Load only the web layer
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean // Mock the service layer
    private UserService userService;
    
    @Test
    @DisplayName("Should return 200 OK with user when found")
    void shouldReturnUser_whenFound() throws Exception {
        // Arrange
        UserDto mockUser = UserDto.builder()
            .id(1L)
            .name("Test User")
            .email("test@example.com")
            .build();
            
        when(userService.findById(1L)).thenReturn(Optional.of(mockUser));
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Test User"))
            .andDo(print());
            
        verify(userService).findById(1L);
    }
}
```

### 3.5 Testing with Slices and MockBean

Spring Boot provides test slice annotations to load only specific parts of the application:

```java
// Test only JPA repositories
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @MockBean // Mock external service used by repository
    private AuditService auditService;
    
    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Test implementation
    }
}

// Test only REST controllers
@WebMvcTest(UserController.class)
class UserControllerSliceTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    @DisplayName("Should validate input")
    void shouldValidateInput() throws Exception {
        // Test implementation
    }
}
```

## 4. Libraries and Dependencies

### 4.1 Core Libraries

- **Spring Data JPA**: For database access and ORM
- **Spring Validation**: Input validation
- **Spring Cache**: Caching capabilities
- **RestClient**: HTTP client for external API integration (Spring 6.1+)
- **SLF4J**: Logging facade for consistent logging implementation
- **Lombok**: Reduce boilerplate code with annotations like `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`
- **Mapstruct**: Object mapping

### 4.2 Database

- Use Spring Data repositories for database operations
- Consider QueryDSL for type-safe dynamic queries
- Implement proper transaction management with `@Transactional`
- Use Flyway or Liquibase for database migrations
- Configure connection pooling (HikariCP recommended)

### 4.3 Logging

- Use SLF4J with Logback as the logging facade
- Configure appropriate log levels for different environments
- Include essential information in log messages (correlation IDs, user contexts)
- Consider using a structured logging format (JSON) for production
- Implement proper log rotation and archiving

```java
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    
    @Override
    public UserDto createUser(UserCreationRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());
        
        try {
            // Business logic
            UserDto mappedUser = new UserDto(); // Simplified for example
            return mappedUser;
        } catch (Exception e) {
            log.error("Failed to create user with email: {}", request.getEmail(), e);
            throw e;
        }
    }
}
```

### 4.4 API Documentation

- Use Springdoc OpenAPI for API documentation
- Document all endpoints with proper descriptions
- Include example requests and responses
- Document possible error responses

### 4.5 HTTP Client with RestClient

Spring Boot 3.4 includes support for the newer RestClient from Spring Framework 6.1, which replaces the older RestTemplate. Use RestClient for making HTTP requests to external services:

```java
@Service
@Slf4j
public class ExternalApiService {
    
    private final RestClient restClient;
    
    public ExternalApiService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
            .baseUrl("https://api.example.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                log.error("Client error: {} {}", response.getStatusCode(), response.getBodyAsString());
                throw new ApiClientException("API client error: " + response.getStatusCode());
            })
            .build();
    }
    
    public ProductDto getProduct(Long productId) {
        log.info("Fetching product with ID: {}", productId);
        return restClient.get()
            .uri("/products/{id}", productId)
            .retrieve()
            .body(ProductDto.class);
    }
    
    public List<ProductDto> searchProducts(String query) {
        log.info("Searching products with query: {}", query);
        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/products/search")
                .queryParam("q", query)
                .build())
            .retrieve()
            .body(new ParameterizedTypeReference<List<ProductDto>>() {});
    }
    
    public ProductDto createProduct(ProductCreationRequest request) {
        log.info("Creating new product: {}", request.getName());
        return restClient.post()
            .uri("/products")
            .body(request)
            .retrieve()
            .body(ProductDto.class);
    }
}
```

Benefits of RestClient over RestTemplate:
- Fluent interface with builder pattern
- Better error handling
- Type safety
- Streamlined request/response processing
- Simplified URI template handling
- Enhanced interceptor support

## 5. Common Pitfalls and Gotchas

### 5.1 Frequent Mistakes to Avoid

- **Not Understanding Spring Boot Concepts**: Ensure solid understanding of Spring and Dependency Injection before diving in
- **Overusing `@Autowired`**: Always prefer constructor injection over field injection
- **Not Using Spring Boot Starters**: Leverage Spring Boot Starters for simplified dependency management
- **Hardcoded Configuration**: Externalize configuration using `application.properties` or `application.yml`
- **Poor Exception Handling**: Implement proper exception handling with meaningful error responses
- **System.out for Logging**: Always use a proper logging framework (SLF4J with Logback)
- **Missing Monitoring**: Set up proper monitoring and alerting

### 5.2 Edge Cases to Consider

- **Null Values**: Handle null values gracefully with proper validation
- **Empty Collections**: Properly handle empty collections
- **Large Datasets**: Optimize performance for large datasets with pagination
- **Concurrency Issues**: Implement proper concurrency controls
- **Network Errors**: Add resilience for handling network errors gracefully

### 5.3 Version Compatibility

- **Spring Boot Version**: Ensure dependencies are compatible with your Spring Boot version
- **Java Version**: Verify Java version compatibility with Spring Boot release
- **Third-Party Libraries**: Check third-party library compatibility

### 5.4 Anti-Patterns to Avoid

- **God Class**: Break down large classes into smaller, focused ones
- **Long Method**: Extract long methods into smaller, single-responsibility methods
- **Field Injection**: Avoid `@Autowired` on fields; use constructor injection or `@RequiredArgsConstructor`:

```java
// AVOID
@Service
public class BadUserService {
    @Autowired
    private UserRepository userRepository;  // Field injection
}

// RECOMMENDED (Option 1)
@Service
public class GoodUserService {
    private final UserRepository userRepository;
    
    // Constructor injection
    public GoodUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// RECOMMENDED (Option 2)
@Service
@RequiredArgsConstructor
public class GoodUserService {
    private final UserRepository userRepository;
    // No constructor needed, Lombok generates it
}
```

- **Tight Coupling**: Use interfaces to reduce dependencies between components
- **Ignoring Exceptions**: Always handle exceptions properly or propagate with meaningful context
- **Excessive Layering**: Avoid creating unnecessary abstraction layers
- **Premature Optimization**: Focus on clean code first, optimize when necessary with measurements

## 6. Performance Optimization Techniques

### 6.1 Database Query Optimization

- Use indexes for frequently queried columns
- Avoid N+1 query problems by using fetch joins or EntityGraph
- Optimize JPQL/HQL queries for better performance
- Use query projections when only a subset of data is needed
- Configure connection pooling (HikariCP) appropriately for your workload

### 6.2 Caching

Implement appropriate caching strategies:

```java
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        log.info("Initializing cache manager");
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            new ConcurrentMapCache("users"),
            new ConcurrentMapCache("products")
        ));
        return cacheManager;
    }
}

@Service
@Slf4j
public class ProductService {
    
    @Cacheable(value = "products", key = "#id")
    public ProductDto findById(Long id) {
        log.info("Finding product by ID: {} (cache miss)", id);
        // Method that would benefit from caching
        return new ProductDto(); // Simplified for example
    }
    
    @CacheEvict(value = "products", key = "#id")
    public void updateProduct(Long id, ProductUpdateRequest request) {
        log.info("Updating product with ID: {}", id);
        // Cache entry will be evicted after update
    }
}
```

### 6.3 Asynchronous Processing

Use `@Async` for non-blocking operations:

```java
@Service
@Slf4j
public class EmailService {
    
    @Async
    public CompletableFuture<Boolean> sendEmail(String to, String subject, String content) {
        log.info("Sending email asynchronously to: {}", to);
        // Asynchronous email sending implementation
        return CompletableFuture.completedFuture(true);
    }
}
```

### 6.4 Pagination

Implement pagination for large datasets:

```java
@RestController
@RequestMapping("/api/v1/products")
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @GetMapping
    public Page<ProductDto> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        
        log.info("Fetching products page: {}, size: {}, sortBy: {}", page, size, sortBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return productService.findAll(pageable);
    }
}
```

### 6.5 Load Testing

- Regularly perform load testing to identify bottlenecks
- Use tools like JMeter, Gatling, or k6 for load testing
- Monitor application performance under different load conditions
- Establish performance baselines and set up alerts for deviations

## 7. Development Environment and Tooling

### 7.1 Recommended Tools

- **IDE**: IntelliJ IDEA, Eclipse, or Visual Studio Code with Spring Boot extensions
- **Build Tool**: Maven or Gradle
- **Version Control**: Git with conventional commit messages
- **API Testing**: Postman or Insomnia
- **Database Client**: DBeaver or similar tools

### 7.2 Code Quality Tools

- **Static Code Analysis**: SonarQube to identify code smells and potential bugs
- **Style Enforcement**: Checkstyle to ensure coding style consistency
- **Code Quality**: PMD, SpotBugs to detect potential problems
- **EditorConfig**: Use EditorConfig to maintain consistent formatting across editors

## 8. General Best Practices

### 8.1 Code Quality

- Follow a consistent coding style guide
- Implement peer code reviews
- Keep methods small and focused on a single responsibility
- Use meaningful names for classes, methods, and variables
- Write comprehensive unit tests
- Document complex logic with clear comments

### 8.2 Performance Optimization

- Use appropriate caching strategies
- Optimize database queries and indexing
- Consider using pagination for large result sets
- Implement asynchronous processing for long-running tasks
- Monitor and optimize JVM memory settings

### 8.3 Documentation

- Maintain updated README files
- Document architecture decisions (ADRs)
- Keep API documentation synchronized with code
- Document configuration options
- Include diagrams for complex systems or workflows

## 9. Additional Considerations

### 9.1 Internationalization and Localization

If your application needs to support multiple languages or regions:

- Use Spring's `MessageSource` for externalized messages
- Configure locale resolution strategies
- Implement locale-specific formatting for dates, numbers, and currencies
- Consider cultural differences in UI design

### 9.2 Advanced API Design Principles

- Implement consistent naming conventions across all endpoints
- Provide standardized filtering, sorting, and pagination mechanisms
- Design comprehensive error response payloads
- Consider implementing HATEOAS for better API discoverability
- Version your APIs appropriately to manage changes

## Conclusion

These guidelines are designed to ensure quality, maintainability, and robustness for Spring Boot 3.4 applications. Teams should adapt these practices to their specific requirements while maintaining the core principles outlined in this document.