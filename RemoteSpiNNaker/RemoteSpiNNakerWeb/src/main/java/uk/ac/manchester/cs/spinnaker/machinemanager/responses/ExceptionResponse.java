package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

public class ExceptionResponse implements Response {
    private String exception;

    public String getException() {
        return exception;
    }

    @JsonSetter("exception")
    public void setException(final JsonNode exception) {
        this.exception = exception.toString();
    }
}
