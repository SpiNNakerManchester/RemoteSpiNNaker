package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to turn off the boards associated with a job.
 */
public class PowerOffJobBoardsCommand extends Command<Integer> {
    /**
     * Create a request to turn off a job's allocated boards.
     *
     * @param jobId
     *            The job to request about.
     */
    public PowerOffJobBoardsCommand(final int jobId) {
        super("power_off_job_boards");
        addArg(jobId);
    }
}
