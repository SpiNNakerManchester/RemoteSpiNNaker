package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get the location of a chip in a job's allocation relative to a
 * machine.
 */
public class WhereIsCommand extends Command<Integer> {
    /**
     * Create a request to locate a chip within a job's allocation.
     *
     * @param jobId
     *            The job to request about.
     * @param chipX
     *            The X coordinate of the chip to ask about.
     * @param chipY
     *            The Y coordinate of the chip to ask about.
     */
    public WhereIsCommand(final int jobId, final int chipX, final int chipY) {
        super("where_is");
        addKwArg("job_id", jobId);
        addKwArg("chip_x", chipX);
        addKwArg("chip_y", chipY);
    }
}
