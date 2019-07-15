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
package uk.ac.manchester.cs.spinnaker.job;

/**
 * Indicates whether a machine was allocated for a job.
 */
public class JobMachineAllocated {

    /**
     * True if the machine has been allocated.
     */
    private boolean allocated = false;

    /**
     * Empty for serialisation.
     */
    public JobMachineAllocated() {
        // Does Nothing
    }

    /**
     * Create an instance.
     *
     * @param allocatedParam
     *            Whether the job was allocated.
     */
    public JobMachineAllocated(final boolean allocatedParam) {
        this.allocated = allocatedParam;
    }

    /**
     * Determine if the job is allocated.
     *
     * @return True if allocated
     */
    public boolean isAllocated() {
        return this.allocated;
    }

    /**
     * Set the job allocation status.
     *
     * @param allocatedParam The allocation status
     */
    public void setAllocated(final boolean allocatedParam) {
        this.allocated = allocatedParam;
    }
}
