package uk.ac.manchester.cs.spinnaker.job;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stack trace provided remotely.
 */
public class RemoteStackTrace {
    private List<RemoteStackTraceElement> elements = new ArrayList<>();

    public RemoteStackTrace() {
        // Does Nothing
    }

    /**
     * Create a basic remote stack trace from an exception.
     *
     * @param throwable
     *            The exception to make the stack trace from.
     */
    public RemoteStackTrace(final Throwable throwable) {
        for (final StackTraceElement element : throwable.getStackTrace()) {
            elements.add(new RemoteStackTraceElement(element));
        }
    }

    public RemoteStackTrace(final List<RemoteStackTraceElement> elements) {
        this.elements = elements;
    }

    public List<RemoteStackTraceElement> getElements() {
        return elements;
    }

    public void setElements(final List<RemoteStackTraceElement> elements) {
        this.elements = elements;
    }
}
