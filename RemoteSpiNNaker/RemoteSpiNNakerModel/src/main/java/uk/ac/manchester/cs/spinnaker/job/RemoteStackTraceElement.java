package uk.ac.manchester.cs.spinnaker.job;

/**
 * Represents a stack trace provided remotely.
 */
public class RemoteStackTraceElement {
    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;

    public RemoteStackTraceElement() {
        // Does Nothing
    }

    /**
     * Create an element of a remoteable stack trace from a standard stack trace
     * element.
     *
     * @param element
     *            The stack trace element to convert.
     */
    public RemoteStackTraceElement(final StackTraceElement element) {
        this.className = element.getClassName();
        this.methodName = element.getMethodName();
        this.fileName = element.getFileName();
        this.lineNumber = element.getLineNumber();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Convert this remote stack trace element to a standard Java stack trace
     * element.
     *
     * @return The created stack trace element.
     */
    public StackTraceElement toSTE() {
        return new StackTraceElement(getClassName(), getMethodName(),
                getFileName(), getLineNumber());
    }
}
