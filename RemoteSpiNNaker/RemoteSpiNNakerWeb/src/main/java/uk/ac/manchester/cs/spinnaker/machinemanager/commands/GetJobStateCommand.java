package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request the state of a job.
 */
public class GetJobStateCommand extends Command<Integer> {
    public GetJobStateCommand(final int jobId) {
        super("get_job_state");
        addArg(jobId);
    }
}
