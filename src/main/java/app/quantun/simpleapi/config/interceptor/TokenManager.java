package app.quantun.simpleapi.config.interceptor;


import app.quantun.simpleapi.config.external.auth.OAuthClient;
import app.quantun.simpleapi.exception.CustomAuthException;
import app.quantun.simpleapi.model.contract.response.TokenResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Service responsible for managing authentication tokens for external service calls.
 * <p>
 * This service handles the lifecycle of authentication tokens, including:
 * <ul>
 *   <li>Token acquisition through the authentication service</li>
 *   <li>Token caching to minimize authentication requests</li>
 *   <li>Thread-safe token renewal using locks</li>
 *   <li>Automatic expiration handling with buffer time</li>
 *   <li>Token invalidation when needed</li>
 * </ul>
 * <p>
 * The TokenManager ensures that valid tokens are always available for
 * authenticated requests while minimizing the number of authentication
 * calls made to the external service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenManager {


    public static final int NUMBER_OF_SECONDS_TO_VALIDATE_TOKEN = 30;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final ScheduledExecutorService resilienceExecutorService;
    private final OAuthClient oAuthClient;
    private final ReentrantLock refreshLock = new ReentrantLock();
    @Value("#{${app.server.external.oauth.headers}}")
    private Map<String, String> headersMap;
    @Value("${app.server.external.oauth.grant-type}")
    private String grantType;
    private String currentToken;
    private Instant expiresAt;


    /**
     * Gets a valid authentication token, refreshing it if necessary.
     * <p>
     * This method ensures that a valid token is always returned by:
     * <ul>
     *   <li>Checking if the current token is still valid</li>
     *   <li>Using a lock to prevent multiple concurrent refresh attempts</li>
     *   <li>Implementing double-check locking pattern for thread safety</li>
     *   <li>Automatically refreshing expired tokens</li>
     * </ul>
     * <p>
     * The method is designed to be efficient by avoiding unnecessary token refreshes
     * while ensuring that all threads get a valid token even under high concurrency.
     *
     * @return A valid authentication token for use with external service calls
     * @see #isTokenValid()
     * @see #refreshToken()
     */
    public String getValidToken() {
        if (isTokenValid()) {
            return currentToken;
        }

        refreshLock.lock();
        try {
            // Double-check after acquiring lock
            if (isTokenValid()) {
                return currentToken;
            }

            log.info("Token expired or not present, requesting new token from OAuth service");
            return refreshToken();

        } finally {
            refreshLock.unlock();
        }
    }

    /**
     * Checks if the current token is valid (exists and not expired).
     * <p>
     * A token is considered valid when:
     * <ul>
     *   <li>It is not null (has been previously acquired)</li>
     *   <li>Its expiration time is known</li>
     *   <li>The current time plus a safety buffer is before the expiration time</li>
     * </ul>
     * <p>
     * The safety buffer ({@value #NUMBER_OF_SECONDS_TO_VALIDATE_TOKEN} seconds) ensures that
     * tokens are refreshed before they actually expire, preventing authentication
     * failures due to clock differences or network latency.
     *
     * @return true if the token exists and is not near expiration, false otherwise
     */
    private boolean isTokenValid() {
        // check each 30 seconds if token is expired
        return currentToken != null && expiresAt != null &&
                Instant.now().plusSeconds(NUMBER_OF_SECONDS_TO_VALIDATE_TOKEN
                ).isBefore(expiresAt);

    }

    /**
     * Forces a token refresh by requesting a new token from the authentication service.
     * <p>
     * This method:
     * <ul>
     *   <li>Creates an authentication request with configured credentials</li>
     *   <li>Calls the authentication service to obtain a new token</li>
     *   <li>Updates the current token and its expiration time</li>
     *   <li>Logs the token refresh and expiration time</li>
     * </ul>
     * <p>
     * The token expiration is calculated based on the requested expiration time
     * from the configuration.
     *
     * @return The newly acquired authentication token
     * @throws app.quantun.simpleapi.exception.CustomAuthException If authentication fails
     */
    private String refreshToken() {
        // Create headers from SpEL evaluated map
        HttpHeaders headers = new HttpHeaders();
        headersMap.forEach(headers::add);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create body
        String body = "grant_type=" + this.grantType;

        try {
            // Wait for the CompletableFuture to complete
            ResponseEntity<TokenResponse> response = callBusinessLogicWithTimeLimiter(headers, body).join();
            currentToken = response.getBody().getAccess_token();

            // Calculate expiration based on requested expiration time
            expiresAt = Instant.now().plusSeconds(response.getBody().getExpires_in());
            log.info("Token successfully refreshed, valid until: {} (expires in {} seconds)",
                    expiresAt, response.getBody().getExpires_in());

            return currentToken;
        } catch (java.util.concurrent.CompletionException ex) {
            // Unwrap the cause if it's a CompletionException
            Throwable cause = ex.getCause();
            if (cause instanceof CustomAuthException) {
                throw (CustomAuthException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new CustomAuthException("Failed to refresh token: " + ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR, ex);
            }
        }
    }


    /**
     * Calls the token generation business logic with resilience patterns applied.
     * <p>
     * This method applies multiple resilience patterns:
     * - Circuit breaker: Prevents repeated calls to failing services
     * - Retry: Attempts to recover from transient failures
     * - Time limiter: Sets a timeout for the operation
     *
     * @param headers HTTP headers for the request
     * @param body    Request body content
     * @return CompletableFuture containing the token response
     */
    public CompletableFuture<ResponseEntity<TokenResponse>> callBusinessLogicWithTimeLimiter(HttpHeaders headers, String body) {
        // Get resilience components
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("authService");
        io.github.resilience4j.retry.Retry retry =
                retryRegistry.retry("authService");
        io.github.resilience4j.timelimiter.TimeLimiter timeLimiter =
                timeLimiterRegistry.timeLimiter("authService");

        log.debug("Preparing resilient token request with circuit breaker, retry, and time limiter");

        // Create the business logic supplier with retry and circuit breaker
        Supplier<ResponseEntity<TokenResponse>> businessLogicSupplier = () ->
                retry.executeSupplier(() ->
                        circuitBreaker.executeSupplier(() ->
                                executeBusinessLogic(headers, body)
                        )
                );

        // Execute asynchronously
        CompletableFuture<ResponseEntity<TokenResponse>> future = CompletableFuture
                .supplyAsync(businessLogicSupplier, resilienceExecutorService);

        // Apply time limiter and handle fallback
        return timeLimiter.executeCompletionStage(resilienceExecutorService, () -> future)
                .toCompletableFuture()
                .handle((result, throwable) -> handleFallback(headers, body, result, throwable));
    }

    /**
     * Executes the core business logic to obtain a token from the OAuth service.
     *
     * @param headers HTTP headers for the request
     * @param body    Request body content
     * @return Response containing the token information
     * @throws CustomAuthException If the token request fails or returns empty response
     */
    private ResponseEntity<TokenResponse> executeBusinessLogic(HttpHeaders headers, String body) {
        log.debug("Requesting token from OAuth service with grant type: {}", grantType);

        ResponseEntity<TokenResponse> response = oAuthClient.getToken(headers, body);

        // Handle empty response body
        if (response.getBody() == null) {
            log.error("OAuth service returned empty response body");
            throw new CustomAuthException("OAuth service returned empty response body", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.debug("Successfully received token response with status: {}", response.getStatusCode());
        return response;
    }

    /**
     * Handles fallback scenarios when token generation encounters errors.
     *
     * @param headers   HTTP headers used in the request
     * @param body      Request body content
     * @param result    Response entity if successful
     * @param throwable Exception that occurred, if any
     * @return Response entity with token information
     * @throws CustomAuthException When token generation fails
     */
    private ResponseEntity<TokenResponse> handleFallback(HttpHeaders headers, String body, ResponseEntity<TokenResponse> result, Throwable throwable) {
        if (throwable == null) {
            if (result == null) {
                log.error("Token generation failed with null result and no exception");
                throw new CustomAuthException("Token generation failed with null result", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return result;
        }

        String errorMessage = throwable.getMessage();
        String requestInfo = String.format("Request [body: %s]", body);

        if (throwable instanceof java.util.concurrent.TimeoutException) {
            log.error("Token generation timed out: {}. {}", errorMessage, requestInfo);
        } else if (throwable instanceof java.util.concurrent.RejectedExecutionException) {
            log.error("Token generation request rejected: {}. {}", errorMessage, requestInfo);
        } else {
            log.error("Token generation failed with unexpected error: {}. {}", errorMessage, requestInfo);
        }

        throw new CustomAuthException(errorMessage, HttpStatus.TOO_MANY_REQUESTS);
    }


    /**
     * Invalidates the current token, forcing the next request to fetch a new one.
     * <p>
     * This method is useful in scenarios such as:
     * <ul>
     *   <li>When a token is rejected by the server before its expected expiration</li>
     *   <li>After security events that might compromise token validity</li>
     *   <li>When manual token refresh is required for testing or administrative purposes</li>
     *   <li>During user logout or session termination</li>
     * </ul>
     * <p>
     * The invalidation is performed by setting both the token and its expiration time to null,
     * which will trigger a refresh on the next call to {@link #getValidToken()}.
     */
    public void invalidateToken() {
        if (currentToken != null) {
            log.info("Invalidating current token, was valid until: {}", expiresAt);
        } else {
            log.info("No active token to invalidate");
        }
        currentToken = null;
        expiresAt = null;
    }
}
