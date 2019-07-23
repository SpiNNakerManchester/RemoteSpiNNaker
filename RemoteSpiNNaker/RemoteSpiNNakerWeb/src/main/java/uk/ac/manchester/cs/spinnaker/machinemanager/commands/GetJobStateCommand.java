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
 * Request the state of a job.
 */
public class GetJobStateCommand extends Command<Integer> {
    /**
     * Create a request to get the state of a job.
     *
     * @param jobId
     *            The job to get the state of.
     */
    public GetJobStateCommand(final int jobId) {
        super("get_job_state");
        addArg(jobId);
    }
}
