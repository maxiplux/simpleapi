package app.quantun.simpleapi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing the scopes supported by the OAuth2 server.
 */
@Getter
@RequiredArgsConstructor
public enum Scope {
    READ("read"),
    WRITE("write");

    private final String value;

    /**
     * Convert a string value to the corresponding enum value.
     *
     * @param value the string value
     * @return the corresponding enum value, or null if not found
     */
    public static Scope fromValue(String value) {
        for (Scope scope : Scope.values()) {
            if (scope.getValue().equals(value)) {
                return scope;
            }
        }
        return null;
    }
}