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
package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get the location of a chip in a job's allocation relative to a
 * machine.
 */
public class WhereIsCommand extends Command<Integer> {
    /**
     * Create a request to locate a chip within a job's allocation.
     *
     * @param jobId
     *            The job to request about.
     * @param chipX
     *            The X coordinate of the chip to ask about.
     * @param chipY
     *            The Y coordinate of the chip to ask about.
     */
    public WhereIsCommand(final int jobId, final int chipX, final int chipY) {
        super("where_is");
        addKwArg("job_id", jobId);
        addKwArg("chip_x", chipX);
        addKwArg("chip_y", chipY);
    }
}
