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

import java.util.List;

import uk.ac.manchester.cs.spinnaker.machine.ChipCoordinates;
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

    /**
     * Turn a machine on or off.
     *
     * @param machine
     *            The machine handle
     * @param powerOn
     *            True to power a machine on, false to turn it off.
     */
    void setMachinePower(SpinnakerMachine machine, boolean powerOn);

    /**
     * Find a chip on a machine.
     *
     * @param machine
     *            The machine handle
     * @param x
     *            The virtual X coordinate of the chip
     * @param y
     *            The virtual Y coordinate of the chip
     * @return The chip location description
     */
    ChipCoordinates getChipCoordinates(SpinnakerMachine machine, int x, int y);
}
