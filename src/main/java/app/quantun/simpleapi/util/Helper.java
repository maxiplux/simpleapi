package app.quantun.simpleapi.util;

import java.util.regex.Pattern;

/**
 * Utility class providing helper methods for common operations across the application.
 * <p>
 * This class contains static utility methods for operations such as:
 * <ul>
 *   <li>Data sanitization to prevent sensitive information exposure</li>
 *   <li>Common string manipulation operations</li>
 *   <li>Security-related utilities</li>
 * </ul>
 * <p>
 * All methods in this class are designed to be thread-safe and have no side effects.
 */
public class Helper {

    /**
     * Regular expression pattern to identify sensitive data in JSON responses.
     * <p>
     * This pattern matches common sensitive field names like password, token, secret, etc.,
     * followed by their values in JSON format.
     */
    private static final Pattern SENSITIVE_DATA_PATTERN =
            Pattern.compile("\"(password|token|secret|key|authorization)\"\\s*:\\s*\"[^\"]+\"",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Sanitizes potentially sensitive information from error responses.
     * <p>
     * This method scans the provided error body for patterns that might contain
     * sensitive information (like passwords, tokens, etc.) and replaces the
     * values with a redacted placeholder while preserving the field names.
     * <p>
     * This is particularly useful for:
     * <ul>
     *   <li>Safely logging error responses</li>
     *   <li>Returning sanitized error details to clients</li>
     *   <li>Debugging issues without exposing sensitive data</li>
     * </ul>
     *
     * @param errorBody The raw error response text, typically JSON
     * @return Sanitized error information with sensitive values redacted
     */
    public static String sanitizeErrorBody(String errorBody) {
        if (errorBody == null || errorBody.isEmpty()) {
            return "[empty response]";
        }

        // Mask sensitive values in JSON responses
        return SENSITIVE_DATA_PATTERN.matcher(errorBody)
                .replaceAll(match -> {
                    // Keep the property name but mask the value
                    String[] parts = match.group().split(":");
                    return parts[0] + ": \"****REDACTED****\"";
                });
    }
}
