package app.quantun.simpleapi.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing the client authentication methods supported by the OAuth2 server.
 */
@Getter
@RequiredArgsConstructor
public enum ClientAuthMethod {
    CLIENT_SECRET_BASIC("client_secret_basic"),
    CLIENT_SECRET_POST("client_secret_post"),
    CLIENT_SECRET_JWT("client_secret_jwt"),
    PRIVATE_KEY_JWT("private_key_jwt"),
    NONE("none");

    private final String value;

    /**
     * Convert a string value to the corresponding enum value.
     *
     * @param value the string value
     * @return the corresponding enum value, or null if not found
     */
    public static ClientAuthMethod fromValue(String value) {
        for (ClientAuthMethod method : ClientAuthMethod.values()) {
            if (method.getValue().equals(value)) {
                return method;
            }
        }
        return null;
    }
}