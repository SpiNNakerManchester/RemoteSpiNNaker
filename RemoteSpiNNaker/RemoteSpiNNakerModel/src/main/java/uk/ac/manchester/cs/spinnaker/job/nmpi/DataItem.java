package uk.ac.manchester.cs.spinnaker.job.nmpi;

/**
 * A reference to some data to be moved into or out of a {@link Job}.
 */
public class DataItem {
    private String url;

    public DataItem() {
        // Does Nothing
    }

    /**
     * Make an instance that wraps a URL. The meaning of the URL depends on the
     * usage of the data item.
     *
     * @param url
     *            The URL to wrap.
     */
    public DataItem(final String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
