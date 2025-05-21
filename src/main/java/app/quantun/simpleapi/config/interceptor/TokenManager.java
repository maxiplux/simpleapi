package app.quantun.simpleapi.config.interceptor;


import app.quantun.simpleapi.config.external.auth.OAuthClient;

import app.quantun.simpleapi.config.restclient.client.AuthClient;
import app.quantun.simpleapi.model.contract.request.AuthRequest;
import app.quantun.simpleapi.model.contract.response.AuthResponse;
import app.quantun.simpleapi.model.contract.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

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


    @Value("#{${app.server.external.oauth.headers}}")
    private Map<String, String> headersMap;


    @Value("${app.server.external.oauth.grant-type}")
    private String getGrantType;


    public static final int NUMBER_OF_SECONDS_TO_VALIDATE_TOKEN = 30;


    private final OAuthClient oAuthClient;

    private final ReentrantLock refreshLock = new ReentrantLock();

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

            log.info("Token expired or not present, requesting new token");
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
        String body = "grant_type="+ this.getGrantType;

        ResponseEntity<TokenResponse> response = oAuthClient.getToken(headers, body);
        currentToken = response.getBody().getAccess_token();
        response.getBody().getExpires_in();

        // Calculate expiration based on requested expiration time
        expiresAt = Instant.now().plusSeconds(response.getBody().getExpires_in() );
        log.info("Token refreshed, valid until: {}", expiresAt);

        return currentToken;
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
        log.info("Invalidating current token");
        currentToken = null;
        expiresAt = null;
    }
}
