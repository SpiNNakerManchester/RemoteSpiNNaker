package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class JobKeepAliveCommand extends Command<Integer> {
    /**
     * Create a request to keep a job alive.
     *
     * @param jobId
     *            The job to ask about.
     */
    public JobKeepAliveCommand(final int jobId) {
        super("job_keepalive");
        addArg(jobId);
    }
}
