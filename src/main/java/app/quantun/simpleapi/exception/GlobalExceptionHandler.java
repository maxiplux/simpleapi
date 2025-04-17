package app.quantun.simpleapi.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Global exception handler that converts exceptions into RFC 7807 Problem Details.
 * This provides standardized error responses across the API.
 * <p>
 * The Problem Details RFC 7807 standard creates a consistent structure for error responses
 * that includes:
 * - type: A URI reference that identifies the problem type
 * - title: A short, human-readable summary of the problem
 * - status: The HTTP status code
 * - detail: A human-readable explanation specific to this occurrence of the problem
 * - instance: A URI reference that identifies the specific occurrence of the problem
 * - additional custom properties as needed
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Pattern to find and mask sensitive data like tokens or credentials
    private static final Pattern SENSITIVE_DATA_PATTERN =
            Pattern.compile("(?i)(password|token|secret|key|authorization|jwt)\\s*[=:]\\s*[^\\s,;]+");

    /**
     * Sanitize potentially sensitive information from exception messages
     */
    private String sanitizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "[no message]";
        }

        // Mask sensitive information
        return SENSITIVE_DATA_PATTERN.matcher(message)
                .replaceAll(match -> {
                    // Keep the field name but mask the value
                    String[] parts = match.group().split("[=:]", 2);
                    return parts[0] + "=****REDACTED****";
                });
    }

    /**
     * Handles circuit breaker exceptions when the circuit is open.
     * <p>
     * This occurs when Resilience4j prevents calls to a failing service to avoid
     * cascade failures. The error response informs clients that the service is
     * temporarily unavailable and should be retried later.
     *
     * @param ex The circuit breaker exception
     * @return A standardized problem detail response
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemDetail> handleCallNotPermittedException(CallNotPermittedException ex) {
        String circuitBreakerName = ex.getCausingCircuitBreakerName();
        String errorId = UUID.randomUUID().toString();
        log.error("Circuit breaker [ID: {}] '{}' is open - service unavailable",
                errorId, circuitBreakerName);
        // Log stack trace only at debug level
        log.debug("Circuit breaker exception details [ID: {}]", errorId, ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service is temporarily unavailable due to excessive failures. Please try again later."
        );

        problemDetail.setType(URI.create("https://api.example.com/errors/service-unavailable"));
        problemDetail.setTitle("Service Temporarily Unavailable");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("circuitBreakerName", circuitBreakerName);
        problemDetail.setProperty("errorId", errorId);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("X-Error-Correlation-ID", errorId).body(problemDetail);
    }

    /**
     * Handles exceptions when calling external services via RestClient.
     * <p>
     * This occurs when there are connectivity issues, timeout issues, or the
     * external service returns an unexpected status code.
     *
     * @param ex The REST client exception
     * @return A standardized problem detail response
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ProblemDetail> handleRestClientException(RestClientException ex) {
        String errorId = UUID.randomUUID().toString();
        String sanitizedMessage = sanitizeMessage(ex.getMessage());

        // Log only minimal info at error level
        log.error("RestClient error [ID: {}]: Error occurred while calling external service", errorId);
        // Log sanitized details at debug level
        log.debug("RestClient error details [ID: {}]: {}", errorId, sanitizedMessage, ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "Error communicating with external service."
        );

        problemDetail.setType(URI.create("https://api.example.com/errors/external-service-error"));
        problemDetail.setTitle("External Service Error");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errorId", errorId);
        problemDetail.setProperty("exceptionClass", ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).header("X-Error-Correlation-ID", errorId).body(problemDetail);
    }

    /**
     * Handles Spring's built-in ErrorResponseException which is used for
     * client errors in Spring Boot 3.x.
     *
     * @param ex The error response exception
     * @return The problem detail directly from the exception
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleErrorResponseException(ErrorResponseException ex) {
        String errorId = UUID.randomUUID().toString();

        // Don't log potentially sensitive details at error level
        log.error("Error response exception [ID: {}]: status={}",
                errorId, ex.getStatusCode());
        // Log details only at debug level
        log.debug("Error response details [ID: {}]: {}",
                errorId, sanitizeMessage(ex.getBody().getDetail()), ex);

        ProblemDetail body = ex.getBody();
        body.setProperty("errorId", errorId);
        body.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(ex.getStatusCode()).header("X-Error-Correlation-ID", errorId).body(body);
    }

    /**
     * Default exception handler for all unhandled exceptions.
     * <p>
     * This provides a generic error response for unexpected issues while
     * avoiding leaking sensitive information to clients.
     *
     * @param ex The unhandled exception
     * @return A standardized problem detail response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        String errorId = UUID.randomUUID().toString();

        // Don't log full exception message at error level
        log.error("Unhandled exception [ID: {}]", errorId);
        // Log details at debug level
        log.debug("Unhandled exception details [ID: {}]: {}",
                errorId, sanitizeMessage(ex.getMessage()), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing your request."
        );

        problemDetail.setType(URI.create("https://api.example.com/errors/internal-server-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errorId", errorId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).header("X-Error-Correlation-ID", errorId).body(problemDetail);
    }

    @ExceptionHandler(CustomAuthException.class)
    public ResponseEntity<ProblemDetail> handleGenericException(CustomAuthException ex) {
        String errorId = UUID.randomUUID().toString();

        // Never log authentication error messages at high levels - they could contain credentials
        log.error("Authentication exception [ID: {}]: status={}",
                errorId, ex.getStatusCode());
        // Log sanitized message at debug level only
        log.debug("Authentication exception details [ID: {}]: {}",
                errorId, sanitizeMessage(ex.getMessage()), ex);

        // Don't include raw error message in response - use generic message
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                ex.getStatusCode(),
                "Authentication error occurred"
        );

        problemDetail.setType(URI.create("https://api.example.com/errors/authentication-error"));
        problemDetail.setTitle("Authentication Error");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errorId", errorId);

        return ResponseEntity.status(ex.getStatusCode()).header("X-Error-Correlation-ID", errorId).body(problemDetail);

    }
}
