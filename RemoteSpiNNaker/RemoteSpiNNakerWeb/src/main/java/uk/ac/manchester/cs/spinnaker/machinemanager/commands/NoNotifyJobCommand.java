package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to not receive notifications about a job.
 */
public class NoNotifyJobCommand extends Command<Integer> {
    public NoNotifyJobCommand(final int jobId) {
        super("no_notify_job");
        addArg(jobId);
    }
}
