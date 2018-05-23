package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class GetJobStateCommand extends Command<Integer> {
    /**
     * Create a request to get the state of a job.
     *
     * @param jobId
     *            The job to get the state of.
     */
    public GetJobStateCommand(final int jobId) {
        super("get_job_state");
        addArg(jobId);
    }
}
