package app.quantun.simpleapi.config.restclient;


import app.quantun.simpleapi.exception.CustomAuthException;
import app.quantun.simpleapi.util.Helper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.UUID;

/**
 * Configuration class for HTTP clients used to communicate with external services.
 * <p>
 * This class configures Spring's RestClient instances for different external services:
 * <ul>
 *   <li>Authentication service: Handles user authentication and token management</li>
 *   <li>Product service: Provides product data and operations</li>
 * </ul>
 * <p>
 * Each client is configured with appropriate error handling, logging, and security measures.
 * The clients use interface-based declarative HTTP clients through Spring's HTTP interface
 * mechanism, which creates proxy implementations at runtime.
 */
@Configuration
@Slf4j
public class HttpClientConfig {

    @Value("${api.auth.url}")
    private String authUrl;
    @Value("${api.product.url}")
    private String productUrl;


    /**
     * Creates and configures a client for the authentication service.
     * <p>
     * This client is used for user authentication operations such as login and token refresh.
     * It includes error handling that:
     * <ul>
     *   <li>Captures and sanitizes error responses</li>
     *   <li>Generates unique request IDs for traceability</li>
     *   <li>Logs errors at appropriate levels (error for status, debug for details)</li>
     *   <li>Converts HTTP errors to application-specific exceptions</li>
     * </ul>
     *
     * @return A configured AuthClient proxy implementation
     * @throws CustomAuthException If there are errors communicating with the auth service
     */
    @Bean
    public AuthClient authClient() {
        // Log only domain, not full URL with potential query parameters
        String authDomain = authUrl.replaceAll("^(https?://[^/]+).*$", "$1");
        log.info("Initializing AuthClient with domain: {}", authDomain);

        RestClient restClient = RestClient.builder()
                .baseUrl(authUrl)
                .defaultStatusHandler(
                        httpStatus -> httpStatus.isError(),
                        (request, response) ->
                        {
                            String requestId = UUID.randomUUID().toString();
                            String errorBody = "";
                            try {
                                errorBody = new String(response.getBody().readAllBytes());
                                errorBody = Helper.sanitizeErrorBody(errorBody);
                                log.error("Auth service error [ID: {}]: Status={}",
                                        requestId, response.getStatusCode());
                                // Log sanitized body at debug level only
                                log.debug("Auth service error details [ID: {}]: {}",
                                        requestId, errorBody);
                            } catch (Exception e) {
                                log.error("Failed to process auth service error [ID: {}]", requestId, e);
                            }
                            throw new CustomAuthException(errorBody, response.getStatusCode());
                        }
                )
                .build();

        log.debug("Creating AuthClient proxy");
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        AuthClient client = factory.createClient(AuthClient.class);
        log.info("AuthClient successfully initialized");
        return client;
    }

    /**
     * Creates and configures a client for the product service.
     * <p>
     * This client is used for product-related operations such as retrieving product details,
     * searching products, and managing product data. It includes:
     * <ul>
     *   <li>Authentication via the provided interceptor</li>
     *   <li>Error handling with sanitized responses</li>
     *   <li>Unique request IDs for traceability</li>
     *   <li>Appropriate logging at different severity levels</li>
     * </ul>
     *
     * @param authInterceptor The interceptor that adds authentication tokens to requests
     * @return A configured ProductClient proxy implementation
     * @throws CustomAuthException If there are errors communicating with the product service
     */
    @Bean
    public ProductClient productClient(AuthenticationInterceptor authInterceptor) {
        // Log only domain, not full URL with potential query parameters
        String productDomain = productUrl.replaceAll("^(https?://[^/]+).*$", "$1");
        log.info("Initializing ProductClient with domain: {}", productDomain);

        RestClient restClient = RestClient.builder()

                .requestInterceptor(authInterceptor)
                .baseUrl(productUrl)
                .defaultStatusHandler(
                        httpStatus -> httpStatus.isError(),
                        (request, response) -> {
                            String requestId = UUID.randomUUID().toString();
                            String errorBody = "";
                            try {
                                errorBody = new String(response.getBody().readAllBytes());
                                errorBody = Helper.sanitizeErrorBody(errorBody);
                                log.error("Product service error [ID: {}]: Status={}",
                                        requestId, response.getStatusCode());
                                // Log sanitized body at debug level only
                                log.debug("Product service error details [ID: {}]: {}",
                                        requestId, errorBody);
                            } catch (Exception e) {
                                log.error("Failed to process product service error [ID: {}]", requestId, e);
                            }
                            throw new CustomAuthException(errorBody, response.getStatusCode());

                        }
                )
                .build();

        log.debug("Creating ProductClient proxy");
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        ProductClient client = factory.createClient(ProductClient.class);
        log.info("ProductClient successfully initialized");
        return client;
    }
}
