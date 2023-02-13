/*
 * Copyright (c) 2014-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
