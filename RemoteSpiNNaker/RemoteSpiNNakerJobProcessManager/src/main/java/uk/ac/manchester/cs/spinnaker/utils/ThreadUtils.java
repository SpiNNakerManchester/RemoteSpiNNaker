package uk.ac.manchester.cs.spinnaker.utils;

public abstract class ThreadUtils {
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
    public static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
