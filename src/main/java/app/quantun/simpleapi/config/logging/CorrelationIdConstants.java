package app.quantun.simpleapi.config.logging;

/**
 * Constants related to correlation IDs.
 */
public final class CorrelationIdConstants {

    /**
     * The name of the HTTP header that contains the correlation ID.
     */
    public static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";

    /**
     * The name of the MDC variable that stores the correlation ID.
     */
    public static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    private CorrelationIdConstants() {
        // Private constructor to prevent instantiation
    }
}