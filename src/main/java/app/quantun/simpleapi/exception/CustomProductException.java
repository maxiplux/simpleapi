package app.quantun.simpleapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * Custom exception for product-related errors.
 * <p>
 * This exception is thrown when product operations fail, such as:
 * <ul>
 *   <li>Product not found during retrieval</li>
 *   <li>Invalid product data during creation or update</li>
 *   <li>Product service availability issues</li>
 *   <li>Permission errors when accessing product resources</li>
 * </ul>
 * <p>
 * The exception includes both an error message and the HTTP status code
 * from the product service, allowing for appropriate error handling
 * and response generation.
 */
@Getter
public class CustomProductException extends RuntimeException {
    /**
     * The HTTP status code associated with this product exception.
     * This is typically the status code returned by the product service.
     */
    private HttpStatusCode statusCode;

    /**
     * Constructs a new product exception with the specified error message and status code.
     *
     * @param message    The detailed error message explaining the product operation failure
     * @param statusCode The HTTP status code associated with this error, typically from the product service
     */
    public CustomProductException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

}
