package app.quantun.simpleapi.config.restclient.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Intercepts HTTP requests to add authentication tokens to outgoing service calls.
 * <p>
 * This interceptor implements Spring's ClientHttpRequestInterceptor to:
 * <ul>
 *   <li>Automatically add valid Bearer tokens to request headers</li>
 *   <li>Skip authentication for auth-related endpoints to prevent circular dependencies</li>
 *   <li>Handle 401 Unauthorized responses by invalidating the current token</li>
 *   <li>Provide appropriate logging for debugging authentication issues</li>
 * </ul>
 * <p>
 * The interceptor works with {@link TokenManager} to ensure that valid tokens
 * are used for all authenticated requests to external services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationInterceptor implements ClientHttpRequestInterceptor {

    private final TokenManager tokenManager;

    /**
     * Checks if the response indicates an unauthorized error (HTTP 401).
     * <p>
     * This method is used to detect when a token has been rejected by the server,
     * which typically happens when the token has expired or been invalidated.
     *
     * @param response The HTTP response from the server
     * @return true if the response status code is 401 Unauthorized, false otherwise
     * @throws IOException If there is an error accessing the response status
     */
    private static boolean unAuthorizedError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().value() == 401;
    }

    /**
     * Determines if the request is not for an authentication endpoint.
     * <p>
     * This method prevents adding authentication headers to authentication requests,
     * which would create a circular dependency (needing a token to get a token).
     *
     * @param request The HTTP request being processed
     * @return true if the request is not for an authentication endpoint, false otherwise
     */
    private static boolean isNotRequestingANewToken(HttpRequest request) {
        return !request.getURI().getPath().contains("/auth/");
    }

    /**
     * Intercepts HTTP requests to add authentication and handle token-related responses.
     * <p>
     * This method implements the core functionality of the interceptor:
     * <ol>
     *   <li>For non-auth endpoints, it adds a Bearer token to the Authorization header</li>
     *   <li>Executes the HTTP request with the modified headers</li>
     *   <li>Checks for 401 Unauthorized responses and invalidates the token if needed</li>
     * </ol>
     * <p>
     * Note that this implementation does not automatically retry requests after
     * a token invalidation. If automatic retry is needed, additional logic would
     * be required to modify and re-execute the request.
     *
     * @param request   The HTTP request to be executed
     * @param body      The body of the request
     * @param execution The execution chain to proceed with after this interceptor
     * @return The response from the HTTP request
     * @throws IOException If an I/O error occurs during request execution
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        // Skip authorization for auth endpoints
        if (isNotRequestingANewToken(request)) {
            request.getHeaders().add(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + tokenManager.getValidToken()
            );
            log.debug("Added Authorization header to request: {}", request.getURI());
        }

        ClientHttpResponse response = execution.execute(request, body);

        // Handle 401 Unauthorized to refresh token
        if (unAuthorizedError(response)) {
            log.info("Received 401 Unauthorized, invalidating token");
            tokenManager.invalidateToken();

            // If we need to retry with a new token automatically,
            // we would add logic here to modify the request and re-execute it
        }

        return response;
    }
}
