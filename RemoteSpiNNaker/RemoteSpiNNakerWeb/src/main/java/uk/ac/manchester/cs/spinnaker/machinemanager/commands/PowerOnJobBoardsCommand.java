package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to turn on the boards associated with a job.
 */
public class PowerOnJobBoardsCommand extends Command<Integer> {
    /**
     * Create a request to turn on a job's allocated boards.
     *
     * @param jobId
     *            The job to request about.
     */
    public PowerOnJobBoardsCommand(final int jobId) {
        super("power_on_job_boards");
        addArg(jobId);
    }
}
