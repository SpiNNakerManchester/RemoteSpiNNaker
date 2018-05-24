package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get notifications about a job.
 */
public class NotifyJobCommand extends Command<Integer> {
    /**
     * Create a request to be notified of changes in job state.
     *
     * @param jobId
     *            The job to request about.
     */
    public NotifyJobCommand(final int jobId) {
        super("notify_job");
        addArg(jobId);
    }
}
