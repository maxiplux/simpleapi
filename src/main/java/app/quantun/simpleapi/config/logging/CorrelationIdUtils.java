package app.quantun.simpleapi.config.logging;

import org.slf4j.MDC;


/**
 * Utility class for working with correlation IDs.
 */
public final class CorrelationIdUtils {

    private CorrelationIdUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the current correlation ID from the MDC.
     *
     * @return the current correlation ID, or null if not present
     */
    public static String getCurrentCorrelationId() {
        return MDC.get(CorrelationIdConstants.CORRELATION_ID_LOG_VAR_NAME);
    }

    /**
     * Sets the correlation ID in the MDC.
     * Note: This should generally be used only by the CorrelationIdFilter.
     *
     * @param correlationId the correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.isEmpty()) {
            MDC.put(CorrelationIdConstants.CORRELATION_ID_LOG_VAR_NAME, correlationId);
        }
    }

    /**
     * Clears the correlation ID from the MDC.
     * Note: This should generally be used only by the CorrelationIdFilter.
     */
    public static void clearCorrelationId() {
        MDC.remove(CorrelationIdConstants.CORRELATION_ID_LOG_VAR_NAME);
    }
}
