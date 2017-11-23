package uk.ac.manchester.cs.spinnaker.utils;

/**
 * A log utility.
 */
public final class Log {

    /**
     * Avoid instantiation.
     */
    private Log() {

        // Does Nothing
    }

    /**
     * Log a message.
     *
     * @param message The message to log
     */
    public static void log(final String message) {
        System.err.println(message);
    }

    /**
     * Log an exception.
     *
     * @param exception The exception to log
     */
    public static void log(final Throwable exception) {
        exception.printStackTrace(System.err);
    }
}
