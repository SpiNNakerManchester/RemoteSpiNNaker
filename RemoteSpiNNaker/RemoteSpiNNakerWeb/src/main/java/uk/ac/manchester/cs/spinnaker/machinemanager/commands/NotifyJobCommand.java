package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class NotifyJobCommand extends Command<Integer> {
    public NotifyJobCommand(final int jobId) {
        super("notify_job");
        addArg(jobId);
    }
}
