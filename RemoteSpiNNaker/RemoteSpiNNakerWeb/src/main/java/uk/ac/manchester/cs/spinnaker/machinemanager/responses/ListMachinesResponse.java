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
package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A response that is the result of listing the machines.
 */
@JsonFormat(shape = ARRAY)
public class ListMachinesResponse {

    /**
     * The list of machines.
     */
    private List<Machine> machines;

    /**
     * Get the list of machines.
     *
     * @return The list of machines
     */
    public List<Machine> getMachines() {
        return machines;
    }

    /**
     * Set the list of machines.
     *
     * @param machinesParam The list of machines to set
     */
    public void setMachines(final List<Machine> machinesParam) {
        this.machines = machinesParam;
    }
}
