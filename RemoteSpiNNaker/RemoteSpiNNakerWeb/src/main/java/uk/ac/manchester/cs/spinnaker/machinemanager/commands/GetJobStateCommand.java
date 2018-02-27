package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request the state of a job.
 */
public class GetJobStateCommand extends Command<Integer> {
	/**
	 * Create a request.
	 *
	 * @param jobId
	 *            The job to get the state of.
	 */
	public GetJobStateCommand(int jobId) {
		super("get_job_state");
		addArg(jobId);
	}
}
