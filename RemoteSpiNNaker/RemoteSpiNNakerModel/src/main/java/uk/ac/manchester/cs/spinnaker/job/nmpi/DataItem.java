package uk.ac.manchester.cs.spinnaker.job.nmpi;

/**
 * A reference to some data to be moved into or out of a {@link Job}.
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
     * Make an instance that wraps a URL. The meaning of the URL depends on the
     * usage of the data item.
     *
     * @param urlParam
     *            The URL to wrap.
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
