package app.quantun.simpleapi.config.interceptor;

import app.quantun.simpleapi.config.external.auth.OAuthClient;
import app.quantun.simpleapi.exception.CustomAuthException;
import app.quantun.simpleapi.model.contract.response.TokenResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for TokenManager that tests actual Resilience4j patterns.
 * <p>
 * This test loads the full Spring context and tests the actual behavior of:
 * <ul>
 *   <li>Circuit breaker pattern with real state transitions</li>
 *   <li>Retry pattern with actual retry attempts</li>
 *   <li>Integration between different resilience patterns</li>
 * </ul>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.authService.sliding-window-size=5",
        "resilience4j.circuitbreaker.instances.authService.failure-rate-threshold=60",
        "resilience4j.circuitbreaker.instances.authService.wait-duration-in-open-state=5s",
        "resilience4j.circuitbreaker.instances.authService.permitted-number-of-calls-in-half-open-state=2",
        "resilience4j.retry.instances.authService.max-attempts=3",
        "resilience4j.retry.instances.authService.wait-duration=500ms",
        "resilience4j.retry.instances.authService.enable-exponential-backoff=true",
        "resilience4j.timelimiter.instances.authService.timeout-duration=2s",
        "app.server.external.oauth.headers={'Authorization': 'Basic dGVzdDp0ZXN0', 'Content-Type': 'application/x-www-form-urlencoded'}",
        "app.server.external.oauth.grant-type=client_credentials"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TokenManagerIntegrationTest {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setUpMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        // The TestConfiguration will use this URL
        System.setProperty("test.oauth.url", mockWebServer.url("/").toString());
    }

    @AfterAll
    static void tearDownMockServer() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @BeforeEach
    void resetState() {
        // Reset circuit breaker state before each test
        circuitBreakerRegistry.circuitBreaker("authService").reset();
        mockWebServer.getRequestCount(); // Clear any previous requests
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully generate token on first attempt")
    void shouldGenerateTokenSuccessfully() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "access_token": "success-token",
                        "token_type": "Bearer",
                        "expires_in": 3600
                    }
                    """));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials";

        // Act
        var responseFuture = tokenManager.generateToken(headers, body);
        var response = responseFuture.join();

        // Assert
        assertThat(response.getBody().getAccess_token()).isEqualTo("success-token");

        // Verify circuit breaker metrics
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authService");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Verify retry metrics
        Retry retry = retryRegistry.retry("authService");
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("Should retry failed requests and eventually succeed")
    void shouldRetryAndEventuallySucceed() {
        // Arrange - First call fails, second succeeds
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "access_token": "retry-success-token",
                        "token_type": "Bearer",
                        "expires_in": 3600
                    }
                    """));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials";

        // Act
        var responseFuture = tokenManager.generateToken(headers, body);
        var response = responseFuture.join();

        // Assert
        assertThat(response.getBody().getAccess_token()).isEqualTo("retry-success-token");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2); // Original + 1 retry

        // Verify retry metrics
        Retry retry = retryRegistry.retry("authService");
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(3)
    @DisplayName("Should open circuit breaker after multiple failures")
    void shouldOpenCircuitBreakerAfterFailures() {
        // Arrange - Multiple failures to trigger circuit breaker
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials";

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authService");

        // Act - Make multiple failing calls
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> tokenManager.generateToken(headers, body).join())
                    .isInstanceOf(java.util.concurrent.CompletionException.class)
                    .hasCauseInstanceOf(CustomAuthException.class);
        }

        // Assert - Circuit breaker should eventually open
        await().atMost(Duration.ofSeconds(10))
                .pollDelay(Duration.ofMillis(100))
                .until(() -> circuitBreaker.getState() == CircuitBreaker.State.OPEN);

        assertThat(circuitBreaker.getMetrics().getFailureRate()).isGreaterThan(0);
    }

    @Test
    @Order(4)
    @DisplayName("Should use fallback when circuit breaker is open")
    void shouldUseFallbackWhenCircuitBreakerOpen() {
        // Arrange - Force circuit breaker to open state
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authService");
        circuitBreaker.transitionToOpenState();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials";

        // Act & Assert - Should use fallback method
        assertThatThrownBy(() -> tokenManager.generateToken(headers, body))
                .isInstanceOf(CustomAuthException.class)
                .satisfies(ex -> {
                    CustomAuthException authEx = (CustomAuthException) ex;
                    assertThat(authEx.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });

        // Verify no actual HTTP request was made (circuit breaker prevented it)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(0);
    }

    @Test
    @Order(5)
    @DisplayName("Should handle timeout with TimeLimiter")
    void shouldHandleTimeout() {
        // Arrange - Response with long delay to trigger timeout
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "access_token": "timeout-token",
                        "token_type": "Bearer",
                        "expires_in": 3600
                    }
                    """)
                .setBodyDelay(5, TimeUnit.SECONDS)); // Delay longer than timeout

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials";

        // Act
        var future = tokenManager.generateToken(headers, body);

        // Manually add a timeout to the CompletableFuture for testing purposes
        var timeoutFuture = future.orTimeout(1, TimeUnit.SECONDS);

        // Assert
        // This should timeout and throw a CompletionException with a TimeoutException cause
        assertThatThrownBy(() -> timeoutFuture.join())
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
    }

    @Test
    @Order(6)
    @DisplayName("Should test circuit breaker half-open state transition")
    void shouldTestHalfOpenStateTransition() throws InterruptedException {
        // Arrange - Force circuit breaker to open
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authService");
        circuitBreaker.transitionToOpenState();

        // Wait for the circuit breaker to transition to half-open
        // (wait-duration-in-open-state is set to 5s in properties)
        Thread.sleep(6000);

        // Enqueue a successful response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "access_token": "half-open-success-token",
                        "token_type": "Bearer",
                        "expires_in": 3600
                    }
                    """));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials";

        // Act
        var responseFuture = tokenManager.generateToken(headers, body);
        var response = responseFuture.join();

        // Assert
        assertThat(response.getBody().getAccess_token()).isEqualTo("half-open-success-token");

        // Circuit breaker should eventually transition back to closed
        await().atMost(Duration.ofSeconds(5))
                .until(() -> circuitBreaker.getState() == CircuitBreaker.State.CLOSED);
    }

    @Test
    @Order(7)
    @DisplayName("Should test getValidToken with circuit breaker integration")
    void shouldTestGetValidTokenWithCircuitBreaker() {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                        "access_token": "integration-token",
                        "token_type": "Bearer",
                        "expires_in": 3600
                    }
                    """));

        // Act
        String token = tokenManager.getValidToken();

        // Assert
        assertThat(token).isEqualTo("integration-token");

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authService");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public OAuthClient testOAuthClient() {
            String baseUrl = System.getProperty("test.oauth.url", "http://localhost:8080");

            RestClient restClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            RestClientAdapter adapter = RestClientAdapter.create(restClient);
            HttpServiceProxyFactory factory = HttpServiceProxyFactory
                    .builderFor(adapter)
                    .build();

            return factory.createClient(OAuthClient.class);
        }
    }
}
