package app.quantun.simpleapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * Custom exception for authentication-related errors.
 * <p>
 * This exception is thrown when authentication operations fail, such as:
 * <ul>
 *   <li>Invalid credentials during login</li>
 *   <li>Token validation failures</li>
 *   <li>Authorization errors when accessing protected resources</li>
 *   <li>Communication errors with the authentication service</li>
 * </ul>
 * <p>
 * The exception includes both an error message and the HTTP status code
 * from the authentication service, allowing for appropriate error handling
 * and response generation.
 */
@Getter
public class CustomAuthInternalException extends RuntimeException {
    /**
     * The HTTP status code associated with this authentication exception.
     * This is typically the status code returned by the authentication service.
     */
    private HttpStatusCode statusCode;

    /**
     * Constructs a new authentication exception with the specified error message and status code.
     *
     * @param message    The detailed error message explaining the authentication failure
     * @param statusCode The HTTP status code associated with this error, typically from the auth service
     */
    public CustomAuthInternalException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

}
