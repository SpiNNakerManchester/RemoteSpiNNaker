package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get machine information relating to a job.
 */
public class GetJobMachineInfoCommand extends Command<Integer> {
    /**
     * Create a request to get information about a job's allocated machine.
     *
     * @param jobId
     *            The job to ask about.
     */
    public GetJobMachineInfoCommand(final int jobId) {
        super("get_job_machine_info");
        addArg(jobId);
    }
}
