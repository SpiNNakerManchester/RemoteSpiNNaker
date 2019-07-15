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
 * Request to create a job.
 */
public class CreateJobCommand extends Command<Integer> {
    /**
     * Create a request to create a job.
     *
     * @param numBoards
     *            The number of boards to request.
     * @param owner
     *            The owner of the job to create.
     */
    public CreateJobCommand(final int numBoards, final String owner) {
        super("create_job");
        addArg(numBoards);
        addKwArg("owner", owner);
    }
}
