package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Command to request machine information.
 */
public class GetJobMachineInfoCommand extends Command<Integer> {

    /**
     * Create the command.
     *
     * @param jobId The ID of the job to get the machine information of.
     */
    public GetJobMachineInfoCommand(final int jobId) {
        super("get_job_machine_info");
        addArg(jobId);
    }
}
