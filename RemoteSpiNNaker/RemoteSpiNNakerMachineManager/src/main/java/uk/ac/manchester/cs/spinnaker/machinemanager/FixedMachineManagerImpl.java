package uk.ac.manchester.cs.spinnaker.machinemanager;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

/**
 * A manager of SpiNNaker machines
 *
 */
public class FixedMachineManagerImpl implements MachineManager {

    /**
     * The queue of available machines
     */
    private Queue<SpinnakerMachine> machinesAvailable =
            new LinkedList<SpinnakerMachine>();

    /**
     * Creates a new MachineManager for a set of machines
     * @param machinesAvailable The machines that are to be managed
     */
    public FixedMachineManagerImpl(List<SpinnakerMachine> machinesAvailable) {
        for (SpinnakerMachine machine : machinesAvailable) {
            this.machinesAvailable.add(machine);
        }
    }

    /**
     * Gets the next machine available, or waits if no machine is available
     *
     * @param n_chips The number of chips required, or -1 for any machine
     * @return The next machine available, or null if the manager is closed
     *         before a machine becomes available
     */
    @Override
    public SpinnakerMachine getNextAvailableMachine(int nChips) {

        // TODO: Actually check the machine has n_chips!

        synchronized (machinesAvailable) {
            while (machinesAvailable.isEmpty()) {
                try {
                    machinesAvailable.wait();
                } catch (InterruptedException e) {

                    // Does Nothing
                }
            }
            return machinesAvailable.poll();
        }
    }

    /**
     * Releases a machine that was previously in use
     * @param machine The machine to release
     */
    @Override
    public void releaseMachine(SpinnakerMachine machine) {
        synchronized (machinesAvailable) {
            machinesAvailable.add(machine);
            machinesAvailable.notifyAll();
        }
    }

    /**
     * Closes the manager
     */
    @Override
    public void close() {
        synchronized (machinesAvailable) {
            machinesAvailable.add(null);
            machinesAvailable.notifyAll();
        }
    }

    @Override
    public boolean isMachineAvailable(SpinnakerMachine machine) {

        // The machine doesn't get taken off this manager
        return true;
    }
}