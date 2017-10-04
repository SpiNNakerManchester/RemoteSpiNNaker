package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to keep a job alive.
 */
public class JobKeepAliveCommand extends Command<Integer> {
	/**
	 * Create a request.
	 *
	 * @param jobId
	 *            The job to ask about.
	 */
	public JobKeepAliveCommand(int jobId) {
		super("job_keepalive");
		addArg(jobId);
	}
}
