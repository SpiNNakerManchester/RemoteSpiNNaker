package uk.ac.manchester.cs.spinnaker.machinemanager;

import java.util.List;

import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

/**
 * A service for managing SpiNNaker boards in a machine.
 */
public interface MachineManager extends AutoCloseable {
	/**
	 * Gets the machines that this manager allocates from.
	 *
	 * @return collection of machines
	 */
	List<SpinnakerMachine> getMachines();

	/**
	 * Get the next available machine of a given size.
	 *
	 * @param nBoards
	 *            The (minimum) number of boards that the machine needs to have.
	 * @return a machine.
	 */
	SpinnakerMachine getNextAvailableMachine(int nBoards);

	/**
	 * Test if a specific machine is available.
	 *
	 * @param machine
	 *            The machine handle
	 * @return true if the machine is available.
	 */
	boolean isMachineAvailable(SpinnakerMachine machine);

	/**
	 * Wait for the machine's availability to change.
	 *
	 * @param machine
	 *            The machine handle
	 * @param waitTime
	 *            Maximum wait time (in milliseconds)
	 * @return Whether the machine state has changed.
	 */
	boolean waitForMachineStateChange(SpinnakerMachine machine, int waitTime);

	/**
	 * Release an allocated machine.
	 *
	 * @param machine
	 *            The machine handle
	 */
	void releaseMachine(SpinnakerMachine machine);
}
