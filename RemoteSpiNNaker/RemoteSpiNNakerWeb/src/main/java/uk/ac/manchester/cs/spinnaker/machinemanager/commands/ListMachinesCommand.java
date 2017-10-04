package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get the known machines from the machine manager.
 */
public class ListMachinesCommand extends Command<String> {
	/**
	 * Create a request.
	 */
	public ListMachinesCommand() {
		super("list_machines");
	}
}
