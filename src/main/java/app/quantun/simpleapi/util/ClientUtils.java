package app.quantun.simpleapi.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for client-related operations.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientUtils {

    /**
     * Default access token time to live in seconds (1 hour).
     */
    public static final int DEFAULT_ACCESS_TOKEN_TIME_TO_LIVE = 3600;

    /**
     * Default refresh token time to live in seconds (30 days).
     */
    public static final int DEFAULT_REFRESH_TOKEN_TIME_TO_LIVE = 2592000;

    /**
     * Calculate the expiration time for a client secret.
     *
     * @param daysValid the number of days the client secret should be valid
     * @return the expiration time as an Instant
     */
    public static Instant calculateClientSecretExpiry(int daysValid) {
        log.debug("Calculating client secret expiry for {} days", daysValid);
        return Instant.now().plus(daysValid, ChronoUnit.DAYS);
    }

    /**
     * Mask a client secret for security purposes.
     *
     * @param clientSecret the client secret to mask
     * @return the masked client secret
     */
    public static String maskClientSecret(String clientSecret) {
        if (clientSecret == null || clientSecret.isEmpty()) {
            return "";
        }

        log.debug("Masking client secret");
        return "[PROTECTED]";
    }
}