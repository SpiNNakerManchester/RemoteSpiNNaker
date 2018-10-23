package uk.ac.manchester.cs.spinnaker.utils;

/**
 * Utilities for working with threads.
 */
public abstract class ThreadUtils {

    /**
     * Avoid creation.
     */
    private ThreadUtils() {
    }

    /**
     * Recommended way of doing "quiet" sleeps.
     *
     * @param delay
     *            How long to sleep for, in milliseconds.
     * @see <a href="https://stackoverflow.com/q/1087475/301832">Stack Overflow
     *      Question: When does Java's Thread.sleep throw
     *      InterruptedException?</a>
     */
    public static void sleep(final long delay) {
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
