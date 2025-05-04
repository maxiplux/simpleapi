package app.quantun.simpleapi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing the authorization grant types supported by the OAuth2 server.
 */
@Getter
@RequiredArgsConstructor
public enum GrantType {
    CLIENT_CREDENTIALS("client_credentials"),
    REFRESH_TOKEN("refresh_token");

    private final String value;

    /**
     * Convert a string value to the corresponding enum value.
     *
     * @param value the string value
     * @return the corresponding enum value, or null if not found
     */
    public static GrantType fromValue(String value) {
        for (GrantType grantType : GrantType.values()) {
            if (grantType.getValue().equals(value)) {
                return grantType;
            }
        }
        return null;
    }
}