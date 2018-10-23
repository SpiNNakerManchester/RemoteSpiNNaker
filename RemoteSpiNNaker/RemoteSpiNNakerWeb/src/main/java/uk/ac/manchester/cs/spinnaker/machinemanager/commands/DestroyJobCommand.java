package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to destroy a job.
 */
public class DestroyJobCommand extends Command<Integer> {
    /**
     * Make a request to destroy a job.
     *
     * @param jobId
     *            The ID of the job.
     */
    public DestroyJobCommand(final int jobId) {
        super("destroy_job");
        addArg(jobId);
    }
}
