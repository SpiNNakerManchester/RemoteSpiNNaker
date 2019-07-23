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
package uk.ac.manchester.cs.spinnaker.nmpi;

import java.io.IOException;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;

/**
 * An interface for things that listen for new jobs.
 */
public interface NMPIQueueListener {
    /**
     * Adds a job to the listener.
     *
     * @param job
     *            The job to add.
     * @throws IOException
     *             If anything goes wrong.
     */
    void addJob(Job job) throws IOException;
}
