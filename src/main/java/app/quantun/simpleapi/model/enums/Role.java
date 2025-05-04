package app.quantun.simpleapi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing the roles/authorities supported by the OAuth2 server.
 */
@Getter
@RequiredArgsConstructor
public enum Role {
    ROLE_CLIENT("ROLE_CLIENT"),
    ROLE_ADMIN("ROLE_ADMIN");

    private final String value;

    /**
     * Convert a string value to the corresponding enum value.
     *
     * @param value the string value
     * @return the corresponding enum value, or null if not found
     */
    public static Role fromValue(String value) {
        for (Role role : Role.values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        return null;
    }
}