package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class CreateJobCommand extends Command<Integer> {
    public CreateJobCommand(final int n_boards, final String owner) {
        super("create_job");
        addArg(n_boards);
        addKwArg("owner", owner);
    }
}
