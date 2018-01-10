package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Command to destroy a job.
 */
public class DestroyJobCommand extends Command<Integer> {

    /**
     * Create the command.
     *
     * @param jobId The ID of the job to destroy.
     */
    public DestroyJobCommand(final int jobId) {
        super("destroy_job");
        addArg(jobId);
    }
}
