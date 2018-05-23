package uk.ac.manchester.cs.spinnaker.machinemanager;

import java.util.List;

import uk.ac.manchester.cs.spinnaker.machine.ChipCoordinates;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

/**
 * A service for managing SpiNNaker boards in a machine.
 */
public interface MachineManager extends AutoCloseable {
    List<SpinnakerMachine> getMachines();

    SpinnakerMachine getNextAvailableMachine(int nBoards);

    boolean isMachineAvailable(SpinnakerMachine machine);

    boolean waitForMachineStateChange(SpinnakerMachine machine, int waitTime);

    void releaseMachine(SpinnakerMachine machine);

    void setMachinePower(SpinnakerMachine machine, boolean powerOn);

    ChipCoordinates getChipCoordinates(SpinnakerMachine machine, int x, int y);
}
