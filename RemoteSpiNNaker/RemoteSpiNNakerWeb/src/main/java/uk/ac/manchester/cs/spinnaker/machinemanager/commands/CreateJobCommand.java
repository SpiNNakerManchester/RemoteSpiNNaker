package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to create a job.
 */
public class CreateJobCommand extends Command<Integer> {
    public CreateJobCommand(final int numBoards, final String owner) {
        super("create_job");
        addArg(numBoards);
        addKwArg("owner", owner);
    }
}
