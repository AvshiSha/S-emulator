package semulator.api;

/**
 * Thrown for load/run errors. Message should be clear and user-facing (English).
 */
public class SEmulatorException extends RuntimeException {
    public SEmulatorException(String message) {
        super(message);
    }

    public SEmulatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
