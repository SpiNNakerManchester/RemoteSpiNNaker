package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to not receive notifications about a job.
 */
public class NoNotifyJobCommand extends Command<Integer> {
	/**
	 * Create a request.
	 *
	 * @param jobId
	 *            The job to request about.
	 */
	public NoNotifyJobCommand(int jobId) {
		super("no_notify_job");
		addArg(jobId);
	}
}
