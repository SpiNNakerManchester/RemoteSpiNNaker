package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get the known machines from the spalloc service.
 */
public class ListMachinesCommand extends Command<String> {
    public ListMachinesCommand() {
        super("list_machines");
    }
}
