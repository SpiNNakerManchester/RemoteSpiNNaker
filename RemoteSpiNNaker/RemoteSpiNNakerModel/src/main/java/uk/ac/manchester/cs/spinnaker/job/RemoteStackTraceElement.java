package uk.ac.manchester.cs.spinnaker.job;

/**
 * Represents a stack trace provided remotely.
 */
public class RemoteStackTraceElement {

    /**
     * The name of the class of the element.
     */
    private String className;

    /**
     * The name of the method of the element.
     */
    private String methodName;

    /**
     * The name of the file of the element.
     */
    private String fileName;

    /**
     * The line number of the element.
     */
    private int lineNumber;

    /**
     * Constructor for serialisation.
     */
    public RemoteStackTraceElement() {
        // Does Nothing
    }

    /**
     * Create a stack trace element from a normal element.
     *
     * @param element The non-remote stack trace element to copy
     */
    public RemoteStackTraceElement(final StackTraceElement element) {
        this.className = element.getClassName();
        this.methodName = element.getMethodName();
        this.fileName = element.getFileName();
        this.lineNumber = element.getLineNumber();
    }

    /**
     * Get the className.
     *
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the className.
     *
     * @param classNameParam the className to set
     */
    public void setClassName(final String classNameParam) {
        this.className = classNameParam;
    }

    /**
     * Get the methodName.
     *
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Sets the methodName.
     *
     * @param methodNameParam the methodName to set
     */
    public void setMethodName(final String methodNameParam) {
        this.methodName = methodNameParam;
    }

    /**
     * Get the fileName.
     *
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the fileName.
     *
     * @param fileNameParam the fileName to set
     */
    public void setFileName(final String fileNameParam) {
        this.fileName = fileNameParam;
    }

    /**
     * Get the lineNumber.
     *
     * @return the lineNumber
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets the lineNumber.
     *
     * @param lineNumberParam the lineNumber to set
     */
    public void setLineNumber(final int lineNumberParam) {
        this.lineNumber = lineNumberParam;
    }

    /**
     * Get the StackTraceElement this represents.
     *
     * @return The stack trace element
     */
    public StackTraceElement toSTE() {
        return new StackTraceElement(getClassName(), getMethodName(),
                getFileName(), getLineNumber());
    }
}
