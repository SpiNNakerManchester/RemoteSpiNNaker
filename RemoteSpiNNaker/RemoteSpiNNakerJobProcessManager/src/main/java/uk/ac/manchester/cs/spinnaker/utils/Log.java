package uk.ac.manchester.cs.spinnaker.utils;

/**
 * A very simple-minded logger.
 */
public abstract class Log {
    private Log() {
    }

    /**
     * Write a message to the log.
     *
     * @param message
     *            The message to write.
     */
    public static void log(final String message) {
        System.err.println(message);
    }

    /**
     * Write an exception to the log.
     *
     * @param exception
     *            The exception to write.
     */
    public static void log(final Throwable exception) {
        exception.printStackTrace(System.err);
    }
}
