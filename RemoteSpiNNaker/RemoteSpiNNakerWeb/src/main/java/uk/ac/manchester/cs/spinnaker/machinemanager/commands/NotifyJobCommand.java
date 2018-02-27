package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get notifications about a job.
 */
public class NotifyJobCommand extends Command<Integer> {
	/**
	 * Create a request.
	 *
	 * @param jobId
	 *            The job to request about.
	 */
	public NotifyJobCommand(int jobId) {
		super("notify_job");
		addArg(jobId);
	}
}
