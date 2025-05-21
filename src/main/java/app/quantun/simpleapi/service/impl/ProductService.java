package app.quantun.simpleapi.service.impl;

import app.quantun.simpleapi.config.restclient.client.ProductClient;
import app.quantun.simpleapi.model.contract.request.ProductRequest;
import app.quantun.simpleapi.model.contract.response.ProductResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for product management operations.
 * <p>
 * This service integrates with an external product service via {@link ProductClient} and
 * applies Resilience4j fault tolerance patterns to ensure system stability:
 * <p>
 * <ul>
 *   <li><b>Circuit Breaker Pattern:</b> Prevents cascading failures by temporarily disabling
 *       calls to the product service if it starts failing consistently. This gives the
 *       external service time to recover and protects our system from being overwhelmed.</li>
 *   <li><b>Retry Pattern:</b> Automatically retries failed operations with backoff delays,
 *       helping to handle transient network issues or temporary service unavailability.</li>
 * </ul>
 * <p>
 * The combined use of these patterns ensures higher availability and resilience
 * for our product operations, even when the external service experiences issues.
 */
//@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductClient productClient;

    private final CircuitBreaker productCircuitBreaker;
    private final Retry productRetry;

    /**
     * Adds a new product to the system with fault tolerance mechanisms.
     * <p>
     * This method applies both circuit breaker and retry patterns in the following order:
     * <ol>
     *   <li>Circuit Breaker: The outer resilience layer determines if the call should be
     *       attempted at all based on recent failure history. If the circuit is open
     *       (due to too many recent failures), the call will fail fast without attempting
     *       to contact the external service.</li>
     *   <li>Retry: If the circuit is closed or half-open, and the initial call fails due to
     *       a transient issue, the retry mechanism will automatically attempt the call again
     *       based on its configured policy (number of retries, backoff delays, etc.).</li>
     * </ol>
     * <p>
     * This layered approach ensures maximum resilience while preventing unnecessary retries
     * when the external service is known to be down.
     *
     * @param title The title of the product to add
     * @param body  The description or body content of the product
     * @return A {@link ProductResponse} containing the created product details
     * @throws io.github.resilience4j.circuitbreaker.CallNotPermittedException if the circuit is open
     */
    public ProductResponse addProduct(String title, String body) {
        log.info("Adding new product: {}", title);

        ProductRequest request = ProductRequest.builder()
                .title(title)
                .body(body)
                .build();

        // Apply circuit breaker and retry patterns
        return productCircuitBreaker.executeSupplier(() ->
                productRetry.executeSupplier(() ->
                        productClient.addProduct(request)
                )
        );
    }
}
