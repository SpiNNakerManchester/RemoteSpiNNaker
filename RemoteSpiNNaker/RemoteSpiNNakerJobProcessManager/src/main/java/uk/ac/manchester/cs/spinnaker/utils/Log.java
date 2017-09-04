package uk.ac.manchester.cs.spinnaker.utils;

public class Log {
    public static void log(final String message) {
        System.err.println(message);
    }

    public static void log(final Throwable exception) {
        exception.printStackTrace(System.err);
    }
}
