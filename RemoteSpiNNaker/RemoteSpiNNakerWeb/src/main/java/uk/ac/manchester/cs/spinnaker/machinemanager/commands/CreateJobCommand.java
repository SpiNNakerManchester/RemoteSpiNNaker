package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to create a job.
 */
public class CreateJobCommand extends Command<Integer> {
	/**
	 * Create a request.
	 *
	 * @param n_boards
	 *            The number of boards to request.
	 * @param owner
	 *            The owner of the job to create.
	 */
	public CreateJobCommand(int n_boards, String owner) {
		super("create_job");
		addArg(n_boards);
		addKwArg("owner", owner);
	}
}
