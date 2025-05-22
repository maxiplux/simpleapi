package app.quantun.simpleapi.config.interceptor;

import app.quantun.simpleapi.config.external.auth.OAuthClient;
import app.quantun.simpleapi.exception.CustomAuthException;
import app.quantun.simpleapi.model.contract.response.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for TokenManager focusing on the generateToken method with Resilience4j.
 * 
 * The generateToken method in TokenManager is annotated with the following Resilience4j annotations:
 * - @CircuitBreaker(name = "authService", fallbackMethod = "authFallback")
 *   This prevents cascading failures by stopping calls to the auth service when it's failing.
 *   If the circuit is open, calls will be directed to the authFallback method.
 * 
 * - @Retry(name = "authService")
 *   This automatically retries failed operations with exponential backoff.
 *   The configuration in application.properties sets maxAttempts=2 for authService.
 * 
 * - @TimeLimiter(name = "authService")
 *   This prevents thread blocking by setting timeouts on external calls.
 *   The configuration in application.properties sets timeoutDuration=3s for authService.
 * 
 * These tests verify the basic functionality of the generateToken method, but the actual
 * Resilience4j behavior would be observed in integration tests or in a running application.
 */
@ExtendWith(MockitoExtension.class)
public class TokenManagerTest {

    @Mock
    private OAuthClient oAuthClient;

    @InjectMocks
    private TokenManager tokenManager;

    private HttpHeaders headers;
    private String body;
    private TokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        // Initialize test data
        headers = new HttpHeaders();
        body = "grant_type=client_credentials";

        tokenResponse = new TokenResponse();
        tokenResponse.setAccess_token("test-token");
        tokenResponse.setExpires_in(3600);

        // Initialize TokenManager fields that would normally be injected by Spring
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("Authorization", "Basic dGVzdDp0ZXN0");
        headersMap.put("Content-Type", "application/x-www-form-urlencoded");
        ReflectionTestUtils.setField(tokenManager, "headersMap", headersMap);
        ReflectionTestUtils.setField(tokenManager, "getGrantType", "client_credentials");
    }

    /**
     * Test that the generateToken method successfully calls the OAuthClient
     * and returns the expected response.
     */
    @Test
    void testGenerateToken_Success() {
        // Arrange
        when(oAuthClient.getToken(any(HttpHeaders.class), anyString()))
            .thenReturn(ResponseEntity.ok(tokenResponse));

        // Act
        ResponseEntity<TokenResponse> response = tokenManager.generateToken(headers, body);

        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.getBody().getAccess_token());
        assertEquals(3600, response.getBody().getExpires_in());
        verify(oAuthClient, times(1)).getToken(any(HttpHeaders.class), anyString());
    }

    /**
     * Test that when the OAuthClient throws an exception, it's properly handled.
     * In a real application with Resilience4j enabled, this would trigger the circuit breaker
     * after enough failures.
     */
    @Test
    void testGenerateToken_Failure() {
        // Arrange
        when(oAuthClient.getToken(any(HttpHeaders.class), anyString()))
            .thenThrow(new CustomAuthException("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE));

        // Act & Assert
        CustomAuthException exception = assertThrows(CustomAuthException.class, () -> 
            tokenManager.generateToken(headers, body));
        assertEquals("Service unavailable", exception.getMessage());
        verify(oAuthClient, times(1)).getToken(any(HttpHeaders.class), anyString());
    }

    /**
     * Test that the authFallback method is called with the correct parameters
     * and returns the expected response.
     * 
     * Note: This test doesn't actually test the Resilience4j circuit breaker functionality,
     * as that would require integration testing with the actual Resilience4j library.
     */
    @Test
    void testAuthFallback() {
        // Arrange
        Exception exception = new RuntimeException("Test exception");

        // Act & Assert
        CustomAuthException authException = assertThrows(CustomAuthException.class, () -> 
            tokenManager.authFallback(headers, body, exception));
        assertEquals("Test exception", authException.getMessage());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, authException.getStatusCode());
    }
}
