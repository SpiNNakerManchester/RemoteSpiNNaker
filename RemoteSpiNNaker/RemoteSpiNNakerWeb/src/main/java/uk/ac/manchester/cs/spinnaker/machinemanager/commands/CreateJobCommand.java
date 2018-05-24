package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to create a job.
 */
public class CreateJobCommand extends Command<Integer> {
    /**
     * Create a request to create a job.
     *
     * @param numBoards
     *            The number of boards to request.
     * @param owner
     *            The owner of the job to create.
     */
    public CreateJobCommand(final int numBoards, final String owner) {
        super("create_job");
        addArg(numBoards);
        addKwArg("owner", owner);
    }
}
