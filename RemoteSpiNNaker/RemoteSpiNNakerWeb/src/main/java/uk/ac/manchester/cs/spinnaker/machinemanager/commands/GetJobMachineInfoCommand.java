package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class GetJobMachineInfoCommand extends Command<Integer> {
    public GetJobMachineInfoCommand(final int jobId) {
        super("get_job_machine_info");
        addArg(jobId);
    }
}
