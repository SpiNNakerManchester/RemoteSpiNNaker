package uk.ac.manchester.cs.spinnaker.job;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stack trace provided remotely.
 */
public class RemoteStackTrace {
	private List<RemoteStackTraceElement> elements = new ArrayList<>();

	/**
	 * Create a basic remote stack trace without stack elements.
	 */
	public RemoteStackTrace() {
		// Does Nothing
	}

	/**
	 * Create a basic remote stack trace from an exception.
	 * 
	 * @param throwable
	 *            The exception to make the stack trace from.
	 */
	public RemoteStackTrace(Throwable throwable) {
		for (StackTraceElement element : throwable.getStackTrace()) {
			elements.add(new RemoteStackTraceElement(element));
		}
	}

	/**
	 * Create a basic remote stack trace from a list of elements.
	 * 
	 * @param throwable
	 *            The exception to make the stack trace from.
	 */
	public RemoteStackTrace(List<RemoteStackTraceElement> elements) {
		this.elements = elements;
	}

	public List<RemoteStackTraceElement> getElements() {
		return elements;
	}

	public void setElements(List<RemoteStackTraceElement> elements) {
		this.elements = elements;
	}
}
