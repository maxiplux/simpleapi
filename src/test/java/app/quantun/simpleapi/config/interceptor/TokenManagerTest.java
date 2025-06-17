package app.quantun.simpleapi.config.interceptor;

import app.quantun.simpleapi.config.external.auth.OAuthClient;
import app.quantun.simpleapi.exception.CustomAuthException;
import app.quantun.simpleapi.model.contract.response.TokenResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    private static MockWebServer mockWebServer;
    @MockitoBean
    private OAuthClient oAuthClient;
    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;


    @BeforeAll
    static void setUpMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
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
        circuitBreakerRegistry.circuitBreaker("authService").reset();
        // The following line doesn't clear requests but gets the count.
        // It's generally harmless if tests manage their enqueued responses and consumed requests correctly.
        mockWebServer.getRequestCount();

        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("Authorization", "Basic dGVzdDp0ZXN0");
        headersMap.put("Content-Type", "application/x-www-form-urlencoded");

        ReflectionTestUtils.setField(tokenManager, "headersMap", headersMap);
        ReflectionTestUtils.setField(tokenManager, "grantType", "client_credentials");
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully generate token when OAuth service responds with valid token")
    void shouldCallBusinessLogicWithTimeLimiter_WhenOAuthServiceRespondsSuccessfully() throws InterruptedException {
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
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(CustomAuthException.class)
                .hasMessageContaining("Client authentication failed"); // This checks CompletionException message which often includes cause's message

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
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(CustomAuthException.class);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle timeout scenarios with Resilience4j TimeLimiter")
    void shouldHandleTimeout() {
        // Arrange - Set a delay longer than the configured TimeLimiter duration (1s)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"access_token\": \"token\", \"expires_in\": 3600}")
                .setBodyDelay(2, TimeUnit.SECONDS)); // 2s delay > 1s TimeLimiter config

        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";

        // Act
        var future = tokenManager.callBusinessLogicWithTimeLimiter(headers, body);

        // Assert
        // The future should complete exceptionally with TimeoutException due to Resilience4j TimeLimiter
        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    @Order(5)
    @DisplayName("Should test authFallback method behavior")
    void shouldTestAuthFallback() {
        // Arrange: No response enqueued, so HTTP calls will fail, triggering retries and then fallback.
        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";

        // Act
        java.util.concurrent.CompletableFuture<ResponseEntity<TokenResponse>> result =
                tokenManager.callBusinessLogicWithTimeLimiter(headers, body);

        // Assert
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .satisfies(completionEx -> {
                    Throwable cause = completionEx.getCause();
                    assertThat(cause).isInstanceOf(CustomAuthException.class);
                    CustomAuthException authEx = (CustomAuthException) cause;
                    assertThat(authEx.getMessage()).contains("Service unavailable");
                    assertThat(authEx.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });
        // You might want to verify mockWebServer.getRequestCount() to ensure retries happened if that's part of the fallback trigger.
        // For example, if retry is 2 attempts: assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        // This depends on how MockWebServer handles requests with no enqueued responses (e.g., if it serves 404s that are recorded).
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
                .isInstanceOf(CompletionException.class); // Specific cause could be a JSON parsing exception

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
                .isInstanceOf(CompletionException.class);

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
        tokenManager.callBusinessLogicWithTimeLimiter(customHeaders, body).join(); // Ensure async call completes

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

        // Act
        String firstCall = tokenManager.getValidToken();
        String secondCall = tokenManager.getValidToken();

        // Assert
        assertThat(firstCall).isEqualTo("cached-token");
        assertThat(secondCall).isEqualTo("cached-token");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);

        RecordedRequest recordedRequest = mockWebServer.takeRequest(); // Verify the single request
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    }

    @Test
    @Order(11)
    @DisplayName("Should invalidate token and fetch new one")
    void shouldInvalidateTokenAndFetchNewOne() { // Removed InterruptedException as takeRequest is not called
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
        // If you need to verify the content of these two requests, you'd use takeRequest() twice
        // and then this method would need `throws InterruptedException`.
    }
}