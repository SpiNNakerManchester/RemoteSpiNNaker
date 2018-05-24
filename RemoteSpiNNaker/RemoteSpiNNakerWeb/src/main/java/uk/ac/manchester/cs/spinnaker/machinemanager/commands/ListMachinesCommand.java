package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get the known machines from the spalloc service.
 */
public class ListMachinesCommand extends Command<String> {
    /** Create a request to list the known SpiNNaker machines. */
    public ListMachinesCommand() {
        super("list_machines");
    }
}
