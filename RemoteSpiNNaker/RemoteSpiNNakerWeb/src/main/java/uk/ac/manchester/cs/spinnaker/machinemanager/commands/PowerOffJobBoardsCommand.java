package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class PowerOffJobBoardsCommand extends Command<Integer> {
    public PowerOffJobBoardsCommand(final int jobId) {
        super("power_off_job_boards");
        addArg(jobId);
    }
}
