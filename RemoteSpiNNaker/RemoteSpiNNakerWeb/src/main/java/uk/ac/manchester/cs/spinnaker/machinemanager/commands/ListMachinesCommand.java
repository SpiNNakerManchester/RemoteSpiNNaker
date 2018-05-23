package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

public class ListMachinesCommand extends Command<String> {
    /** Create a request to list the known SpiNNaker machines. */
    public ListMachinesCommand() {
        super("list_machines");
    }
}
