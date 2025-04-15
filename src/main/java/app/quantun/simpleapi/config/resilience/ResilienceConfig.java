package app.quantun.simpleapi.config.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Resilience4j fault tolerance patterns.
 * <p>
 * Resilience4j is a lightweight fault tolerance library inspired by Netflix Hystrix
 * but designed for functional programming and Java 8. This class configures the following
 * Resilience4j components:
 * <p>
 * <ul>
 *   <li><b>Circuit Breakers:</b> Prevent cascading failures by temporarily disabling calls to
 *       failing services. Circuit breakers have three states:
 *       <ul>
 *         <li>CLOSED: All calls pass through (normal operation)</li>
 *         <li>OPEN: All calls fail fast without hitting the remote service</li>
 *         <li>HALF_OPEN: Limited test calls are allowed to check if the service has recovered</li>
 *       </ul>
 *   </li>
 *   <li><b>Retry Mechanisms:</b> Automatically retry failed operations with configurable backoff
 *       strategies. Retries can use:
 *       <ul>
 *         <li>Fixed backoff: Wait a fixed time between retries</li>
 *         <li>Exponential backoff: Increase wait time exponentially</li>
 *         <li>Random backoff: Add jitter to prevent thundering herd problems</li>
 *       </ul>
 *   </li>
 * </ul>
 * <p>
 * The actual circuit breaker and retry configurations (failure thresholds, timeout values,
 * retry counts, etc.) are defined in application.properties or application.yml using the
 * Resilience4j Spring Boot configuration properties.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Creates a circuit breaker for the authentication service.
     * <p>
     * The circuit breaker will track failures when calling the auth service and
     * temporarily disable calls if the failure threshold is exceeded. This prevents
     * cascading failures and allows the auth service time to recover.
     * <p>
     * Configuration parameters such as failure rate threshold, wait duration in open state,
     * and permitted calls in half-open state are loaded from properties with the prefix:
     * {@code resilience4j.circuitbreaker.instances.authService}
     *
     * @param circuitBreakerRegistry The registry that manages all circuit breakers
     * @return A configured CircuitBreaker instance for the auth service
     */
    @Bean
    public CircuitBreaker authCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("authService");
    }

    /**
     * Creates a circuit breaker for the product service.
     * <p>
     * The circuit breaker will track failures when calling the product service and
     * temporarily disable calls if the failure threshold is exceeded. This helps maintain
     * system stability when the product service is experiencing issues.
     * <p>
     * Configuration parameters such as failure rate threshold, wait duration in open state,
     * and permitted calls in half-open state are loaded from properties with the prefix:
     * {@code resilience4j.circuitbreaker.instances.productService}
     *
     * @param circuitBreakerRegistry The registry that manages all circuit breakers
     * @return A configured CircuitBreaker instance for the product service
     */
    @Bean
    public CircuitBreaker productCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("productService");
    }

    /**
     * Creates a retry mechanism for the authentication service.
     * <p>
     * This retry will automatically attempt to repeat failed auth service calls
     * according to the configured retry policy. This is useful for handling transient
     * failures in network communication or temporary service unavailability.
     * <p>
     * Configuration parameters such as max attempts, backoff policy, and retry exceptions
     * are loaded from properties with the prefix:
     * {@code resilience4j.retry.instances.authService}
     *
     * @param retryRegistry The registry that manages all retry configurations
     * @return A configured Retry instance for the auth service
     */
    @Bean
    public Retry authRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("authService");
    }

    /**
     * Creates a retry mechanism for the product service.
     * <p>
     * This retry will automatically attempt to repeat failed product service calls
     * according to the configured retry policy. Retries help improve reliability
     * by automatically recovering from temporary failures.
     * <p>
     * Configuration parameters such as max attempts, backoff policy, and retry exceptions
     * are loaded from properties with the prefix:
     * {@code resilience4j.retry.instances.productService}
     *
     * @param retryRegistry The registry that manages all retry configurations
     * @return A configured Retry instance for the product service
     */
    @Bean
    public Retry productRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("productService");
    }
}
