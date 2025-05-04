package app.quantun.simpleapi.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Provides consistent error responses for different types of exceptions.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    public static final String TIMESTAMP = "timestamp";

    /**
     * Handle constraint violation exceptions (validation errors).
     *
     * @param ex the exception
     * @return a problem detail with validation errors
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("Constraint violation: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation error: " + ex.getMessage());
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setProperty(TIMESTAMP, Instant.now());

        return problemDetail;
    }

    /**
     * Handle method argument not valid exceptions (validation errors from @Valid).
     *
     * @param ex the exception
     * @return a problem detail with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("Method argument not valid: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation error");
        problemDetail.setTitle("Method Argument Not Valid");
        problemDetail.setProperty(TIMESTAMP, Instant.now());
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    /**
     * Handle access denied exceptions.
     *
     * @param ex the exception
     * @return a problem detail with access denied error
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied: " + ex.getMessage());
        problemDetail.setTitle("Access Denied");
        problemDetail.setProperty(TIMESTAMP, Instant.now());

        return problemDetail;
    }

    /**
     * Handle client not found exceptions.
     *
     * @param ex the exception
     * @return a problem detail with not found error
     */
    @ExceptionHandler(ClientNotFoundException.class)
    public ProblemDetail handleClientNotFoundException(ClientNotFoundException ex) {
        log.error("Client not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Client Not Found");
        problemDetail.setProperty(TIMESTAMP, Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(ClientAlreadyExists.class)
    public ProblemDetail handleClientAlreadyExists(ClientAlreadyExists ex) {
        log.error("Client not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Client Already Exists");
        problemDetail.setProperty(TIMESTAMP, Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty(TIMESTAMP, Instant.now());

        return problemDetail;
    }
}
