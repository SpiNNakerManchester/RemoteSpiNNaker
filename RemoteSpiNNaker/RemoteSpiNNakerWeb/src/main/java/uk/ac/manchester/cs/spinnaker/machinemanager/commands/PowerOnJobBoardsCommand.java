package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class PowerOnJobBoardsCommand extends Command<Integer> {
    public PowerOnJobBoardsCommand(final int jobId) {
        super("power_on_job_boards");
        addArg(jobId);
    }
}
