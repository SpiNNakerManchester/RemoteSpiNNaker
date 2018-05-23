package uk.ac.manchester.cs.spinnaker.utils;

/**
 * A very simple-minded logger.
 */
public abstract class Log {
    private Log() {
    }

    public static void log(final String message) {
        System.err.println(message);
    }

    public static void log(final Throwable exception) {
        exception.printStackTrace(System.err);
    }
}
