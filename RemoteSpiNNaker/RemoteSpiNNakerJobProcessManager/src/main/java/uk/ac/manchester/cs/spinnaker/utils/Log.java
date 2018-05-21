package uk.ac.manchester.cs.spinnaker.utils;

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
