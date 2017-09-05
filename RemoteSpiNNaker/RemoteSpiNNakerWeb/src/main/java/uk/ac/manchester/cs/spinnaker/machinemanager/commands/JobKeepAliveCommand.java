package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class JobKeepAliveCommand extends Command<Integer> {
    public JobKeepAliveCommand(final int jobId) {
        super("job_keepalive");
        addArg(jobId);
    }
}
