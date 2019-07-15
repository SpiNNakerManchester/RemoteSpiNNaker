/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.cs.spinnaker.machinemanager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.cs.spinnaker.machine.ChipCoordinates;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

/**
 * A manager of directly-connected SpiNNaker machines.
 */
public class FixedMachineManagerImpl implements MachineManager {
    /**
     * The queue of available machines.
     */
    private final Set<SpinnakerMachine> machinesAvailable = new HashSet<>();

    /**
     * The set of machine allocated.
     */
    private final Set<SpinnakerMachine> machinesAllocated = new HashSet<>();

    /**
     * Lock to avoid concurrent modification in different threads.
     */
    private final Object lock = new Object();

    /**
     * True when the manager is finished.
     */
    private boolean done = false;

    /**
     * Sets the initial set of machines that are available.
     *
     * @param machines
     *            the collection of machines to use
     */
    @Value("${machines}")
    void setInitialMachines(final List<SpinnakerMachine> machines) {
        machinesAvailable.addAll(machines);
    }

    @Override
    public List<SpinnakerMachine> getMachines() {
        final List<SpinnakerMachine> machines = new ArrayList<>();
        synchronized (lock) {
            machines.addAll(machinesAvailable);
            machines.addAll(machinesAllocated);
        }
        return machines;
    }

    @Override
    public SpinnakerMachine getNextAvailableMachine(final int nBoards) {
        try {
            synchronized (lock) {
                SpinnakerMachine machine;
                while (!done) {
                    machine = getLargeEnoughMachine(nBoards);
                    if (machine != null) {
                        // Move the machine from available to allocated
                        machinesAvailable.remove(machine);
                        machinesAllocated.add(machine);
                        return machine;
                    }
                    // If no machine was found, wait for something to change
                    lock.wait();
                }
            }
        } catch (final InterruptedException e) {
        }
        return null;
    }

    /**
     * Get a machine with at least the given number of boards.
     *
     * @param nBoards The number of boards required.
     * @return A machine big enough, or null of none.
     */
    private SpinnakerMachine getLargeEnoughMachine(final int nBoards) {
        for (final SpinnakerMachine nextMachine : machinesAvailable) {
            if (nextMachine.getnBoards() >= nBoards) {
                return nextMachine;
            }
        }
        return null;
    }

    @Override
    public void releaseMachine(final SpinnakerMachine machine) {
        synchronized (lock) {
            machinesAllocated.remove(machine);
            machinesAvailable.add(machine);
            lock.notifyAll();
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            done = true;
            lock.notifyAll();
        }
    }

    @Override
    public boolean isMachineAvailable(final SpinnakerMachine machine) {
        synchronized (lock) {
            return !machinesAvailable.contains(machine);
        }
    }

    @Override
    public boolean waitForMachineStateChange(final SpinnakerMachine machine,
            final int waitTime) {
        synchronized (lock) {
            final boolean isAvailable = machinesAvailable.contains(machine);
            try {
                lock.wait(waitTime);
            } catch (final InterruptedException e) {
                // Does Nothing
            }
            return machinesAvailable.contains(machine) != isAvailable;
        }
    }

    @Override
    public void setMachinePower(
            final SpinnakerMachine machine, final boolean powerOn) {
        // Does Nothing in this implementation
    }

    @Override
    public ChipCoordinates getChipCoordinates(final SpinnakerMachine machine,
            final int x, final int y) {
        return new ChipCoordinates(0, 0, 0);
    }
}
