package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A response that is the successful result of a request.
 */
public class ReturnResponse implements Response {

    /**
     * The value returned.
     */
    private String returnValue;

    /**
     * Get the value returned.
     *
     * @return The value
     */
    public String getReturnValue() {
        return returnValue;
    }

    /**
     * Set the value returned.
     *
     * @param returnValueParam The value to set
     */
    @JsonSetter("return")
    public void setReturnValue(final JsonNode returnValueParam) {
        this.returnValue = returnValueParam.toString();
    }
}
