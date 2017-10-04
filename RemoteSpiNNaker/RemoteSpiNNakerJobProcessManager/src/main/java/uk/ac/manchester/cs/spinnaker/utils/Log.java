package uk.ac.manchester.cs.spinnaker.utils;

/**
 * A very simple-minded logger.
 */
public abstract class Log {
	// Mark as no instances
	private Log() {
	}

	/**
	 * Write a message to the log.
	 *
	 * @param message
	 *            The message to write.
	 */
	public static void log(String message) {
		System.err.println(message);
	}

	/**
	 * Write an exception to the log.
	 *
	 * @param exception
	 *            The exception to write.
	 */
	public static void log(Throwable exception) {
		exception.printStackTrace(System.err);
	}
}
