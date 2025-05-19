# SimpleAPI Project Guidelines

This document provides essential information for developers working on the SimpleAPI project.

## Build/Configuration Instructions

### Prerequisites

- Java 17 or higher
- Gradle 8.x or higher
- Docker and Docker Compose (for running dependencies)

### Setting Up the Development Environment

1. **Start the required services using Docker Compose**:
   ```bash
   docker-compose up -d
   ```
   This will start:
    - PostgreSQL database on port 5432
    - IBM MQ on port 1414 (with web console on port 9443)

2. **Build the application**:
   ```bash
   ./gradlew build
   ```

3. **Run the application**:
   ```bash
   ./gradlew bootRun
   ```

4. **Access the application**:
    - API: http://localhost:8080
    - API Documentation: http://localhost:8080/api-docs
    - Swagger UI: http://localhost:8080/swagger-ui.html

### Configuration

The application uses Spring Boot's configuration system. Key configuration files:

- `application.properties`: Main configuration file with database, IBM MQ, and resilience4j settings
- `docker-compose.yml`: Configuration for containerized dependencies

To override configuration for local development, create an `application-local.properties` file and run with:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Testing Information

### Running Tests

1. **Run all tests**:
   ```bash
   ./gradlew test
   ```

2. **Run a specific test class**:
   ```bash
   ./gradlew test --tests "app.quantun.simpleapi.controller.MqMessageTestControllerTest"
   ```

3. **Run a specific test method**:
   ```bash
   ./gradlew test --tests "app.quantun.simpleapi.controller.MqMessageTestControllerTest.testHomeEndpoint"
   ```

### Test Structure

The project uses:

- JUnit 5 (Jupiter) as the testing framework
- Spring Boot Test for integration testing
- MockMvc for testing REST controllers
- Testcontainers for integration tests with real dependencies

### Creating New Tests

#### Unit Tests

For unit tests, follow these guidelines:

1. Create a test class in the same package as the class being tested
2. Name the test class with the pattern `[ClassUnderTest]Test`
3. Use appropriate annotations:
    - `@SpringBootTest` for integration tests
    - `@AutoConfigureMockMvc` when testing controllers with MockMvc

Example unit test for a controller:

```java
@SpringBootTest
@AutoConfigureMockMvc
public class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public JmsTemplate jmsTemplate() {
            return Mockito.mock(JmsTemplate.class);
        }
        
        @Bean
        public String queue(@Value("${ibm.mq.queue:DEV.QUEUE.1}") String queueName) {
            return queueName;
        }
    }

    @Test
    public void testHomeEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("Simple API with IBM MQ integration is running!"));
    }
}
```

#### Integration Tests

For integration tests with external dependencies:

1. Use `@SpringBootTest` with appropriate configuration
2. Configure Testcontainers for database or message queue testing
3. Use the `TestcontainersConfiguration` class to define container configurations

## Additional Development Information

### Code Style

- The project follows standard Java code style conventions
- Lombok is used to reduce boilerplate code (getters, setters, constructors)
- Use constructor injection (via `@RequiredArgsConstructor`) rather than field injection

### Resilience Patterns

The application implements several resilience patterns using Resilience4j:

1. **Circuit Breaker**: Prevents cascading failures by stopping calls to failing services
2. **Retry**: Automatically retries failed operations with exponential backoff
3. **Timeout**: Prevents thread blocking by setting timeouts on external calls

See the detailed configuration in `application.properties`.

### IBM MQ Integration

The application integrates with IBM MQ for message processing:

1. Messages can be sent using the `JmsTemplate` (see `HomeController.sendMessage()`)
2. Incoming messages are processed by listeners (see `MqMessageListener`)

### Debugging

For debugging IBM MQ issues:

- Access the MQ web console at http://localhost:9443/ibmmq/console
    - Username: admin
    - Password: admin123
- View queue contents and manage the queue manager

For application debugging:

- Enable debug logging by adding `logging.level.app.quantun=DEBUG` to application.properties
- Use Spring Boot Actuator endpoints at `/actuator` for runtime information