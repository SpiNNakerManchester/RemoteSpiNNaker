package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class DestroyJobCommand extends Command<Integer> {
    public DestroyJobCommand(final int jobId) {
        super("destroy_job");
        addArg(jobId);
    }
}
