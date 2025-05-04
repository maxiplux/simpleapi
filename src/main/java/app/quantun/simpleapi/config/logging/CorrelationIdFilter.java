package app.quantun.simpleapi.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


/**
 * Filter that adds a correlation ID to each request.
 * The correlation ID is either extracted from the request header or generated if not present.
 * It is then stored in the MDC for logging and added to the response header.
 */
@Component
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Check if correlation ID exists in the request header
            String correlationId = request.getHeader(CorrelationIdConstants.CORRELATION_ID_HEADER_NAME);

            // If not present, generate a new one
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = generateCorrelationId();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using existing correlation ID from request: {}", correlationId);
            }

            // Store the correlation ID in the MDC
            CorrelationIdUtils.setCorrelationId(correlationId);

            // Add the correlation ID to the response header
            response.addHeader(CorrelationIdConstants.CORRELATION_ID_HEADER_NAME, correlationId);

            // Continue with the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Always clear the MDC after the request is processed
            CorrelationIdUtils.clearCorrelationId();
        }
    }

    /**
     * Generates a unique correlation ID.
     *
     * @return a unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
