package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A response to a request that indicates a failure.
 */
public class ExceptionResponse implements Response {

    /**
     * The exception to report.
     */
    private String exception;

    /**
     * Get the exception to report.
     *
     * @return The exception
     */
    public String getException() {
        return exception;
    }

    /**
     * Set the exception to report.
     *
     * @param exceptionParam The exception to set
     */
    @JsonSetter("exception")
    public void setException(final JsonNode exceptionParam) {
        this.exception = exceptionParam.toString();
    }
}
