package uk.ac.manchester.cs.spinnaker.utils;

/**
 * Utilities for working with threads.
 */
public abstract class ThreadUtils {
	private ThreadUtils() {
	}

	/**
	 * Recommended way of doing "quiet" sleeps.
	 *
	 * @param delay
	 *            the length of time to sleep in milliseconds
	 * @see <a href="https://stackoverflow.com/q/1087475/301832">Stack Overflow
	 *      question</a>
	 */
	public static void sleep(long delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
	}
}
