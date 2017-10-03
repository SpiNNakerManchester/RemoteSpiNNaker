package uk.ac.manchester.cs.spinnaker.utils;

public abstract class ThreadUtils {
	private ThreadUtils() {
	}

	/**
	 * Recommended way of doing "quiet" sleeps.
	 *
	 * @param delay
	 *            the length of time to sleep in milliseconds
	 * @see <a href=
	 *      "http://stackoverflow.com/questions/1087475/when-does-javas-thread-sleep-throw-interruptedexception"
	 *      >stackoverflow.com/.../when-does-javas-thread-sleep-throw-interruptedexception</a>
	 */
	public static void sleep(long delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
