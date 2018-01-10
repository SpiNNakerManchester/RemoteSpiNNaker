package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Command to get the state of a job.
 */
public class GetJobStateCommand extends Command<Integer> {

    /**
     * Create the command.
     *
     * @param jobId The ID of the job
     */
    public GetJobStateCommand(final int jobId) {
        super("get_job_state");
        addArg(jobId);
    }
}
