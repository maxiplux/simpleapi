package app.quantun.simpleapi.exception;

/**
 * Exception thrown when a client is not found.
 */
public class ClientAlreadyExists extends RuntimeException {

    /**
     * Constructs a new ClientNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public ClientAlreadyExists(String message) {
        super(message);
    }

    /**
     * Constructs a new ClientNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ClientAlreadyExists(String message, Throwable cause) {
        super(message, cause);
    }
}