package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to turn off the boards associated with a job.
 */
public class PowerOffJobBoardsCommand extends Command<Integer> {
    public PowerOffJobBoardsCommand(final int jobId) {
        super("power_off_job_boards");
        addArg(jobId);
    }
}
