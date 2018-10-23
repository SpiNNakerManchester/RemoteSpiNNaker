package uk.ac.manchester.cs.spinnaker.job;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stack trace provided remotely.
 */
public class RemoteStackTrace {

    /**
     * The elements of the stack trace.
     */
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
    public RemoteStackTrace(final Throwable throwable) {
        for (final StackTraceElement element : throwable.getStackTrace()) {
            elements.add(new RemoteStackTraceElement(element));
        }
    }

    /**
     * Create a basic remote stack trace from a list of elements.
     *
     * @param elementsParam
     *            The elements to make the stack trace from.
     */
    public RemoteStackTrace(final List<RemoteStackTraceElement> elementsParam) {
        this.elements = elementsParam;
    }

    /**
     * Get the elements.
     *
     * @return the elements
     */
    public List<RemoteStackTraceElement> getElements() {
        return elements;
    }

    /**
     * Set the elements.
     *
     * @param elementsParam The elements to set
     */
    public void setElements(final List<RemoteStackTraceElement> elementsParam) {
        this.elements = elementsParam;
    }
}
