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
     * Constructor for serialisation.
     */
    public RemoteStackTrace() {
        // Does Nothing
    }

    /**
     * Create a remote stack trace from a throwable.
     *
     * @param throwable A throwable from which to construct the trace.
     */
    public RemoteStackTrace(final Throwable throwable) {
        for (final StackTraceElement element : throwable.getStackTrace()) {
            elements.add(new RemoteStackTraceElement(element));
        }
    }

    /**
     * Create a remote stack trace using elements.
     *
     * @param elementsParam The elements that make up the trace.
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
