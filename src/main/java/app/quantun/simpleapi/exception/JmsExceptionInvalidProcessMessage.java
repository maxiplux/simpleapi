package app.quantun.simpleapi.exception;

public class JmsExceptionInvalidProcessMessage extends RuntimeException {
    public JmsExceptionInvalidProcessMessage(String message) {
        super(message);
    }
}
