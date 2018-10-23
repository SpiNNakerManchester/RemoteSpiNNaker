package uk.ac.manchester.cs.spinnaker.job.nmpi;

/**
 * A message indicating that the queue is empty.
 */
public class QueueEmpty implements QueueNextResponse {

    /**
     * Any warning returned.
     */
    private String warning;

    /**
     * Get the warning.
     *
     * @return The warning
     */
    public String getWarning() {
        return warning;
    }

    /**
     * Set the warning.
     *
     * @param warningParam The warning to set
     */
    public void setWarning(final String warningParam) {
        this.warning = warningParam;
    }
}
