package uk.ac.manchester.cs.spinnaker.model;

/**
 * A Neuromorphic Platform Interface log core.
 */
public class NMPILog {

    /**
     * The content of the log.
     */
    private StringBuilder content;

    /**
     * Gets the current log contents.
     *
     * @return The log contents, or <tt>null</tt> if the log is not yet
     *         initialised.
     */
    public String getContent() {
        if (content == null) {
            return null;
        }
        return content.toString();
    }

    /**
     * Set the content.
     *
     * @param contentParam The content to set
     */
    public void setContent(final String contentParam) {
        this.content = new StringBuilder(contentParam);
    }

    /**
     * Append the string to the log.
     *
     * @param contentParam
     *            The string to append.
     */
    public void appendContent(final String contentParam) {
        if (this.content == null) {
            this.content = new StringBuilder(contentParam);
        } else {
            this.content.append(contentParam);
        }
    }
}
