package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get notifications about a job.
 */
public class NotifyJobCommand extends Command<Integer> {
    public NotifyJobCommand(final int jobId) {
        super("notify_job");
        addArg(jobId);
    }
}
