package uk.ac.manchester.cs.spinnaker.job.nmpi;

/**
 * An item of data in a job.
 */
public class DataItem {

    /**
     * The item URL.
     */
    private String url;

    /**
     * Creates an empty item of data.
     */
    public DataItem() {
        // Does Nothing
    }

    /**
     * Creates an item of data with a URL.
     *
     * @param urlParam The URL of the item
     */
    public DataItem(final String urlParam) {
        this.url = urlParam;
    }

    /**
     * Get the URL of the item of data.
     *
     * @return The URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the URL of the item of data.
     *
     * @param urlParam The URL
     */
    public void setUrl(final String urlParam) {
        this.url = urlParam;
    }
}
