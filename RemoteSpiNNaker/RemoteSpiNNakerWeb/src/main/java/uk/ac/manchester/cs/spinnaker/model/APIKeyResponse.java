package uk.ac.manchester.cs.spinnaker.model;

/**
 * POJO holding the response for a query for an API key.
 */
public class APIKeyResponse {

    /**
     * The API Key.
     */
    private String key;

    /**
     * Get the API Key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the API key.
     *
     * @param keyParam The key to set
     */
    public void setKey(final String keyParam) {
        this.key = keyParam;
    }
}
