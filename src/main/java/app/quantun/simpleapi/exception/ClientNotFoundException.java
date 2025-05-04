package app.quantun.simpleapi.exception;

/**
 * Exception thrown when a client is not found.
 */
public class ClientNotFoundException extends RuntimeException {

    /**
     * Constructs a new ClientNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public ClientNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ClientNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ClientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}