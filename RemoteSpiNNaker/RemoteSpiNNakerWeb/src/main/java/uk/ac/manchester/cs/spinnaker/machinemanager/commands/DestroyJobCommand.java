package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to destroy a job.
 */
public class DestroyJobCommand extends Command<Integer> {
	/**
	 * Make a request.
	 *
	 * @param jobId The ID of the job.
	 */
	public DestroyJobCommand(int jobId) {
		super("destroy_job");
		addArg(jobId);
	}
}
