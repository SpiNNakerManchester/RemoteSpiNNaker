package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * A command for creating a job.
 */
public class CreateJobCommand extends Command<Integer> {

    /**
     * Create a job command.
     *
     * @param nBoards The number of boards to request
     * @param owner The owner of the job
     */
    public CreateJobCommand(final int nBoards, final String owner) {
        super("create_job");
        addArg(nBoards);
        addKwArg("owner", owner);
    }
}
