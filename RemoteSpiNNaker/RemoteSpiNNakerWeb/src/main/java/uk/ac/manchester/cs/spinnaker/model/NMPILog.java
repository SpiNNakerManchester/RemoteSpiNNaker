package uk.ac.manchester.cs.spinnaker.model;

public class NMPILog {
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

    public void setContent(final String content) {
        this.content = new StringBuilder(content);
    }

    /**
     * Append the string to the log.
     *
     * @param content
     *            The string to append.
     */
    public void appendContent(final String content) {
        if (this.content == null) {
            this.content = new StringBuilder(content);
        } else {
            this.content.append(content);
        }
    }
}
