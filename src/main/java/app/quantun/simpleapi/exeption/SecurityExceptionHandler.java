package app.quantun.simpleapi.exeption;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.security.SignatureException;
import java.time.Instant;

@RestControllerAdvice
public class SecurityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidBearerTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidBearerToken(InvalidBearerTokenException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid bearer token: The provided token is malformed or cannot be verified");

        enrichProblemDetail(problemDetail, request);
        problemDetail.setProperty("error_type", "invalid_token");
        problemDetail.setTitle("Authentication Error");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed: " + (ex.getMessage() != null ? ex.getMessage() : "Invalid credentials"));

        enrichProblemDetail(problemDetail, request);
        problemDetail.setProperty("error_type", "authentication_failure");
        problemDetail.setTitle("Authentication Error");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(JwtValidationException.class)
    public ResponseEntity<ProblemDetail> handleJwtValidation(JwtValidationException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "JWT validation failed: " + (ex.getMessage() != null ? ex.getMessage() : "Token is not valid"));

        enrichProblemDetail(problemDetail, request);
        problemDetail.setProperty("error_type", "jwt_validation_error");
        problemDetail.setProperty("validation_errors", ex.getErrors());
        problemDetail.setTitle("JWT Validation Error");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ProblemDetail> handleJwtException(JwtException ex, WebRequest request) {
        String errorMessage = ex.getMessage();
        String errorType = "jwt_error";

        // More specific error categorization based on message content
        if (errorMessage != null) {
            if (errorMessage.contains("expired")) {
                errorType = "token_expired";
                errorMessage = "Your authentication token has expired. Please login again.";
            } else if (errorMessage.contains("signature")) {
                errorType = "invalid_signature";
                errorMessage = "Token signature verification failed. The token may have been tampered with.";
            } else if (errorMessage.contains("algorithm")) {
                errorType = "algorithm_mismatch";
                errorMessage = "Token signing algorithm doesn't match the expected algorithm.";
            }
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                errorMessage != null ? errorMessage : "JWT processing error occurred");

        enrichProblemDetail(problemDetail, request);
        problemDetail.setProperty("error_type", errorType);
        problemDetail.setTitle("JWT Error");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Access denied: You don't have permission to access this resource");

        enrichProblemDetail(problemDetail, request);
        problemDetail.setProperty("error_type", "access_denied");
        problemDetail.setTitle("Authorization Error");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    private void enrichProblemDetail(ProblemDetail problemDetail, WebRequest request) {
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("request_id", request.getSessionId());
    }

//    @ExceptionHandler(ExpiredJwtException.class)
//    @ResponseStatus(HttpStatus.UNAUTHORIZED)
//    public ErrorResponse handleExpiredJwtException(ExpiredJwtException ex) {
//        return new ErrorResponse("JWT is expired", HttpStatus.UNAUTHORIZED.value());
//    }
//
//    @ExceptionHandler(MalformedJwtException.class)
//    @ResponseStatus(HttpStatus.UNAUTHORIZED)
//    public ErrorResponse handleMalformedJwtException(MalformedJwtException ex) {
//        return new ErrorResponse("JWT is malformed", HttpStatus.UNAUTHORIZED.value());
//    }

    @ExceptionHandler(SignatureException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ProblemDetail> handleSignatureException(SignatureException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "JWT signature is invalid");

        enrichProblemDetail(problemDetail, request);
        problemDetail.setProperty("error_type", "invalid_signature");
        problemDetail.setTitle("Authentication Error");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

}
