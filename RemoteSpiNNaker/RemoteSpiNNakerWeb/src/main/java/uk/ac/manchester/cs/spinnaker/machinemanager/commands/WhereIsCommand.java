package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class WhereIsCommand extends Command<Integer> {
    public WhereIsCommand(final int jobId, final int chipX, final int chipY) {
        super("where_is");
        addKwArg("job_id", jobId);
        addKwArg("chip_x", chipX);
        addKwArg("chip_y", chipY);
    }
}
