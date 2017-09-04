package uk.ac.manchester.cs.spinnaker.job.nmpi;

public class DataItem {
    private String url;

    public DataItem() {
        // Does Nothing
    }

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
