/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
     * Create an element of a remote stack trace from a standard stack trace
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
