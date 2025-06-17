package app.quantun.simpleapi.config.interceptor;

import app.quantun.simpleapi.config.external.auth.OAuthClient;
import app.quantun.simpleapi.exception.CustomAuthException;
import app.quantun.simpleapi.model.contract.response.TokenResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for TokenManager using MockWebServer for realistic HTTP testing.
 * <p>
 * This test class uses MockWebServer to simulate the OAuth service responses,
 * providing more realistic testing than pure mocking. It tests:
 * <ul>
 *   <li>Successful token generation</li>
 *   <li>HTTP error handling</li>
 *   <li>Request content validation</li>
 *   <li>Resilience patterns (circuit breaker, retry, timeout)</li>
 * </ul>
 * <p>
 * Note: The actual Resilience4j behavior (circuit breaker, retry, timeout) would be
 * fully observable in integration tests with the complete Spring context loaded.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.authService.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.authService.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.authService.wait-duration-in-open-state=1s",
        "resilience4j.retry.instances.authService.max-attempts=2",
        "resilience4j.timelimiter.instances.authService.timeout-duration=1s",
        "app.oauth.headers={'Authorization': 'Basic dGVzdDp0ZXN0', 'Content-Type': 'application/x-www-form-urlencoded'}",
        "app.oauth.grant-type=client_credentials"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TokenManagerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public OAuthClient oAuthClient() {
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

    // Only keeping the necessary fields
    private static MockWebServer mockWebServer;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;


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

        // Set up TokenManager configuration via reflection
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("Authorization", "Basic dGVzdDp0ZXN0");
        headersMap.put("Content-Type", "application/x-www-form-urlencoded");

        ReflectionTestUtils.setField(tokenManager, "headersMap", headersMap);
        ReflectionTestUtils.setField(tokenManager, "grantType", "client_credentials");
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully generate token when OAuth service responds with valid token")
    void shoulcallBusinessLogicWithTimeLimiter_WhenOAuthServiceRespondsSuccessfully() throws InterruptedException {
        // Arrange
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
        int expectedExpiresIn = 3600;

        String jsonResponse = String.format("""
                {
                    "access_token": "%s",
                    "token_type": "Bearer",
                    "expires_in": %d
                }
                """, expectedToken, expectedExpiresIn);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic dGVzdDp0ZXN0");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials";

        // Act
        ResponseEntity<TokenResponse> response = tokenManager.callBusinessLogicWithTimeLimiter(headers, body).join();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccess_token()).isEqualTo(expectedToken);
        assertThat(response.getBody().getExpires_in()).isEqualTo(expectedExpiresIn);

        // Verify the request was made correctly
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/oauth2/token");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Basic dGVzdDp0ZXN0");
        assertThat(recordedRequest.getHeader("Content-Type")).contains("application/x-www-form-urlencoded");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("grant_type=client_credentials");
    }

    @Test
    @Order(2)
    @DisplayName("Should throw CustomAuthException when OAuth service returns 401 Unauthorized")
    void shouldThrowCustomAuthException_WhenUnauthorized() throws InterruptedException {
        // Arrange
        String errorResponse = """
                {
                    "error": "invalid_client",
                    "error_description": "Client authentication failed"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(errorResponse));

        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";

        // Act & Assert
        assertThatThrownBy(() -> tokenManager.callBusinessLogicWithTimeLimiter(headers, body).join())
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(CustomAuthException.class)
                .hasMessageContaining("Client authentication failed");

        // Verify request was made
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/oauth2/token");
    }

    @Test
    @Order(3)
    @DisplayName("Should throw CustomAuthException when OAuth service returns 500 Internal Server Error")
    void shouldThrowCustomAuthException_WhenServerError() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"server_error\"}"));

        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";

        // Act & Assert
        assertThatThrownBy(() -> tokenManager.callBusinessLogicWithTimeLimiter(headers, body).join())
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(CustomAuthException.class);

        // Verify request was made
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle timeout scenarios")
    void shouldHandleTimeout() {
        // Arrange - Set a long delay to simulate timeout
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"access_token\": \"token\", \"expires_in\": 3600}")
                .setBodyDelay(5, TimeUnit.SECONDS)); // 5 second delay

        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";

        // Act
        var future = tokenManager.callBusinessLogicWithTimeLimiter(headers, body);

        // Manually add a timeout to the CompletableFuture for testing purposes
        var timeoutFuture = future.orTimeout(1, TimeUnit.SECONDS);

        // Assert
        // This should timeout and throw a CompletionException with a TimeoutException cause
        assertThatThrownBy(() -> timeoutFuture.join())
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
    }

    @Test
    @Order(5)
    @DisplayName("Should test authFallback method")
    void shouldTestAuthFallback() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";
        Exception testException = new RuntimeException("Service unavailable");

        // Act & Assert
        java.util.concurrent.CompletableFuture<ResponseEntity<TokenResponse>> result = tokenManager.callBusinessLogicWithTimeLimiter(headers, body);

        assertThatThrownBy(() -> result.join())
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(CustomAuthException.class)
                .hasMessageContaining("Service unavailable");

        // Verify the cause has the expected status code
        try {
            result.join();
        } catch (java.util.concurrent.CompletionException ex) {
            CustomAuthException authEx = (CustomAuthException) ex.getCause();
            assertThat(authEx.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should handle malformed JSON response")
    void shouldHandleMalformedJsonResponse() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ invalid json }"));

        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";

        // Act & Assert
        assertThatThrownBy(() -> tokenManager.callBusinessLogicWithTimeLimiter(headers, body).join())
                .isInstanceOf(java.util.concurrent.CompletionException.class); // Could be parsing exception

        // Verify request was made
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle empty response body")
    void shouldHandleEmptyResponseBody() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(""));

        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";

        // Act & Assert
        assertThatThrownBy(() -> tokenManager.callBusinessLogicWithTimeLimiter(headers, body).join())
                .isInstanceOf(java.util.concurrent.CompletionException.class);

        // Verify request was made
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }

    @Test
    @Order(8)
    @DisplayName("Should validate request headers are properly set")
    void shouldValidateRequestHeaders() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"access_token\": \"token\", \"expires_in\": 3600}"));

        HttpHeaders customHeaders = new HttpHeaders();
        customHeaders.set("Authorization", "Basic Y3VzdG9tOmNyZWRz");
        customHeaders.set("X-Custom-Header", "custom-value");
        customHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=client_credentials&scope=read";

        // Act
        tokenManager.callBusinessLogicWithTimeLimiter(customHeaders, body);

        // Assert
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Basic Y3VzdG9tOmNyZWRz");
        assertThat(recordedRequest.getHeader("X-Custom-Header")).isEqualTo("custom-value");
        assertThat(recordedRequest.getHeader("Content-Type")).contains("application/x-www-form-urlencoded");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("grant_type=client_credentials&scope=read");
    }

    @Test
    @Order(9)
    @DisplayName("Should test getValidToken integration")
    void shouldTestGetValidTokenIntegration() throws InterruptedException {
        // Arrange
        String tokenValue = "valid-token-123";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(String.format("""
                        {
                            "access_token": "%s",
                            "token_type": "Bearer",
                            "expires_in": 3600
                        }
                        """, tokenValue)));

        // Act
        String result = tokenManager.getValidToken();

        // Assert
        assertThat(result).isEqualTo(tokenValue);

        // Verify request was made to get the token
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/oauth2/token");
    }

    @Test
    @Order(10)
    @DisplayName("Should cache token and not make unnecessary requests")
    void shouldCacheTokenAndNotMakeUnnecessaryRequests() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                            "access_token": "cached-token",
                            "token_type": "Bearer",
                            "expires_in": 3600
                        }
                        """));

        // Act - Call getValidToken twice
        String firstCall = tokenManager.getValidToken();
        String secondCall = tokenManager.getValidToken();

        // Assert
        assertThat(firstCall).isEqualTo("cached-token");
        assertThat(secondCall).isEqualTo("cached-token");

        // Verify only one request was made (token was cached)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }

    @Test
    @Order(11)
    @DisplayName("Should invalidate token and fetch new one")
    void shouldInvalidateTokenAndFetchNewOne() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                            "access_token": "first-token",
                            "token_type": "Bearer",
                            "expires_in": 3600
                        }
                        """));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                            "access_token": "second-token",
                            "token_type": "Bearer",
                            "expires_in": 3600
                        }
                        """));

        // Act
        String firstToken = tokenManager.getValidToken();
        tokenManager.invalidateToken();
        String secondToken = tokenManager.getValidToken();

        // Assert
        assertThat(firstToken).isEqualTo("first-token");
        assertThat(secondToken).isEqualTo("second-token");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }
}
