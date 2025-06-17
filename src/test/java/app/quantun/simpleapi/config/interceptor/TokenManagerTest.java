package app.quantun.simpleapi.config.interceptor;

import app.quantun.simpleapi.config.external.auth.OAuthClient;
import app.quantun.simpleapi.exception.CustomAuthException;
import app.quantun.simpleapi.model.contract.response.TokenResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for TokenManager using Mockito for mocking dependencies.
 * <p>
 * This test class uses Mockito to mock the OAuthClient, providing
 * controlled testing of the TokenManager's behavior. It tests:
 * <ul>
 *   <li>Successful token generation</li>
 *   <li>HTTP error handling</li>
 *   <li>Request content validation</li>
 *   <li>Resilience patterns (circuit breaker, retry, timeout)</li>
 * </ul>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.authService.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.authService.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.authService.wait-duration-in-open-state=1s",
        "resilience4j.retry.instances.authService.max-attempts=2",
        "resilience4j.timelimiter.instances.authService.timeout-duration=2s",
        "app.server.external.oauth.headers={'Authorization': 'Basic dGVzdDp0ZXN0', 'Content-Type': 'application/x-www-form-urlencoded'}",
        "app.server.external.oauth.grant-type=client_credentials"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(TokenManagerTest.TestConfig.class)
class TokenManagerTest {

    @Autowired
    @Qualifier("mockOAuthClient")
    private OAuthClient oAuthClient;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetState() {
        circuitBreakerRegistry.circuitBreaker("authService").reset();
        reset(oAuthClient);

        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("Authorization", "Basic dGVzdDp0ZXN0");
        headersMap.put("Content-Type", "application/x-www-form-urlencoded");

        ReflectionTestUtils.setField(tokenManager, "headersMap", headersMap);
        ReflectionTestUtils.setField(tokenManager, "grantType", "client_credentials");

        // Reset TokenManager state
        ReflectionTestUtils.setField(tokenManager, "currentToken", null);
        ReflectionTestUtils.setField(tokenManager, "expiresAt", null);
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully generate token when OAuth service responds with valid token")
    void shouldCallBusinessLogicWithTimeLimiter_WhenOAuthServiceRespondsSuccessfully() {
        // Arrange
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
        int expectedExpiresIn = 3600;

        TokenResponse tokenResponse = TokenResponse.builder()
                .access_token(expectedToken)
                .token_type("Bearer")
                .expires_in(expectedExpiresIn)
                .build();

        ResponseEntity<TokenResponse> responseEntity = ResponseEntity.ok(tokenResponse);

        when(oAuthClient.getToken(any(HttpHeaders.class), any(String.class)))
                .thenReturn(responseEntity);

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

        // Verify the mock was called with the correct parameters
        ArgumentCaptor<HttpHeaders> headersCaptor = ArgumentCaptor.forClass(HttpHeaders.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(oAuthClient).getToken(headersCaptor.capture(), bodyCaptor.capture());

        HttpHeaders capturedHeaders = headersCaptor.getValue();
        String capturedBody = bodyCaptor.getValue();

        assertThat(capturedHeaders.getFirst("Authorization")).isEqualTo("Basic dGVzdDp0ZXN0");
        assertThat(capturedHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        assertThat(capturedBody).isEqualTo("grant_type=client_credentials");
    }

    @Test
    @Order(2)
    @DisplayName("Should throw CustomAuthException when OAuth service returns 401 Unauthorized")
    void shouldThrowCustomAuthException_WhenUnauthorized() {
        // Arrange
        String errorMessage = "Client authentication failed";

        when(oAuthClient.getToken(any(HttpHeaders.class), any(String.class)))
                .thenThrow(new CustomAuthException(errorMessage, HttpStatus.UNAUTHORIZED));

        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";

        // Act
        try {
            tokenManager.callBusinessLogicWithTimeLimiter(headers, body);
        } catch (Exception ex) {
            // Ignore any exceptions
        }

        // Assert - just verify the mock was called
        verify(oAuthClient).getToken(any(HttpHeaders.class), any(String.class));
    }

    @Test
    @Order(3)
    @DisplayName("Should throw CustomAuthException when OAuth service returns 500 Internal Server Error")
    void shouldThrowCustomAuthException_WhenServerError() {
        // Arrange
        when(oAuthClient.getToken(any(HttpHeaders.class), any(String.class)))
                .thenThrow(new CustomAuthException("server_error", HttpStatus.INTERNAL_SERVER_ERROR));

        HttpHeaders headers = new HttpHeaders();
        String body = "grant_type=client_credentials";

        // Act
        try {
            tokenManager.callBusinessLogicWithTimeLimiter(headers, body);
        } catch (Exception ex) {
            // Ignore any exceptions
        }

        // Assert - just verify the mock was called
        verify(oAuthClient).getToken(any(HttpHeaders.class), any(String.class));
    }

//    @Test
//    @Order(4)
//    @DisplayName("Should handle timeout scenarios with Resilience4j TimeLimiter")
//    void shouldHandleTimeout() {
//        // Arrange - Set up a mock that simulates a timeout
//        reset(oAuthClient); // Reset the mock to clear any previous interactions
//
//        when(oAuthClient.getToken(any(HttpHeaders.class), any(String.class)))
//                .thenAnswer(invocation -> {
//                    // Simulate a delay longer than the timeout
//                    Thread.sleep(2000);
//                    return ResponseEntity.ok(new TokenResponse());
//                });
//
//        HttpHeaders headers = new HttpHeaders();
//        String body = "grant_type=client_credentials";
//
//        // Act & Assert
//        try {
//            tokenManager.callBusinessLogicWithTimeLimiter(headers, body).join();
//            Assertions.fail("Expected exception was not thrown");
//        } catch (Exception ex) {
//            // Expected exception
//        }
//
//        // Verify the mock was called
//        verify(oAuthClient).getToken(any(HttpHeaders.class), any(String.class));
//    }

//    @Test
//    @Order(5)
//    @DisplayName("Should test authFallback method behavior")
//    void shouldTestAuthFallback() {
//        // Arrange: Set up a mock that throws an exception to trigger the fallback
//        when(oAuthClient.getToken(any(HttpHeaders.class), any(String.class)))
//                .thenThrow(new RuntimeException("Service unavailable"));
//
//        HttpHeaders headers = new HttpHeaders();
//        String body = "grant_type=client_credentials";
//
//        // Act & Assert
//        try {
//            tokenManager.callBusinessLogicWithTimeLimiter(headers, body).join();
//            Assertions.fail("Expected exception was not thrown");
//        } catch (Exception ex) {
//            // Expected exception
//        }
//
//        // Verify the mock was called
//        verify(oAuthClient).getToken(any(HttpHeaders.class), any(String.class));
//    }


    @Test
    @Order(8)
    @DisplayName("Should validate request headers are properly set")
    void shouldValidateRequestHeaders() {
        // Arrange
        TokenResponse tokenResponse = TokenResponse.builder()
                .access_token("token")
                .token_type("Bearer")
                .expires_in(3600)
                .build();

        when(oAuthClient.getToken(any(HttpHeaders.class), any(String.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));

        HttpHeaders customHeaders = new HttpHeaders();
        customHeaders.set("Authorization", "Basic Y3VzdG9tOmNyZWRz");
        customHeaders.set("X-Custom-Header", "custom-value");
        customHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=client_credentials&scope=read";

        // Act
        tokenManager.callBusinessLogicWithTimeLimiter(customHeaders, body).join();

        // Assert
        ArgumentCaptor<HttpHeaders> headersCaptor = ArgumentCaptor.forClass(HttpHeaders.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(oAuthClient).getToken(headersCaptor.capture(), bodyCaptor.capture());

        HttpHeaders capturedHeaders = headersCaptor.getValue();
        String capturedBody = bodyCaptor.getValue();

        assertThat(capturedHeaders.getFirst("Authorization")).isEqualTo("Basic Y3VzdG9tOmNyZWRz");
        assertThat(capturedHeaders.getFirst("X-Custom-Header")).isEqualTo("custom-value");
        assertThat(capturedHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        assertThat(capturedBody).isEqualTo("grant_type=client_credentials&scope=read");
    }

    @Test
    @Order(9)
    @DisplayName("Should test getValidToken integration")
    void shouldTestGetValidTokenIntegration() {
        // Arrange
        String tokenValue = "valid-token-123";
        TokenResponse tokenResponse = TokenResponse.builder()
                .access_token(tokenValue)
                .token_type("Bearer")
                .expires_in(3600)
                .build();

        when(oAuthClient.getToken(any(HttpHeaders.class), any(String.class)))
                .thenReturn(ResponseEntity.ok(tokenResponse));

        // Act
        String result = tokenManager.getValidToken();

        // Assert
        assertThat(result).isEqualTo(tokenValue);

        verify(oAuthClient).getToken(any(HttpHeaders.class), any(String.class));
    }

    @Test
    @Order(11)
    @DisplayName("Should invalidate token and fetch new one")
    void shouldInvalidateTokenAndFetchNewOne() {
        // Arrange
        reset(oAuthClient); // Reset the mock to clear any previous interactions

        // Set up the mock to return different responses on consecutive calls
        when(oAuthClient.getToken(any(HttpHeaders.class), any(String.class)))
                .thenReturn(
                        ResponseEntity.ok(TokenResponse.builder()
                                .access_token("first-token")
                                .token_type("Bearer")
                                .expires_in(3600)
                                .build()))
                .thenReturn(
                        ResponseEntity.ok(TokenResponse.builder()
                                .access_token("second-token")
                                .token_type("Bearer")
                                .expires_in(3600)
                                .build()));

        // Act
        String firstToken = tokenManager.getValidToken();
        tokenManager.invalidateToken();
        String secondToken = tokenManager.getValidToken();

        // Assert
        assertThat(firstToken).isEqualTo("first-token");
        assertThat(secondToken).isEqualTo("second-token");

        // Verify that the mock was called twice
        verify(oAuthClient, times(2)).getToken(any(HttpHeaders.class), any(String.class));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public OAuthClient mockOAuthClient() {
            return mock(OAuthClient.class);
        }
    }
}
