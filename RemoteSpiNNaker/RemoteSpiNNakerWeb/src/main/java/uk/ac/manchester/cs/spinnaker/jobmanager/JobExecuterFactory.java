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
package uk.ac.manchester.cs.spinnaker.jobmanager;

import java.io.IOException;
import java.net.URL;

/**
 * A factory for creating job executers.
 *
 * @see LocalJobExecuterFactory
 * @see XenVMExecuterFactory
 */
public interface JobExecuterFactory {
    /**
     * Creates a new {@link JobExecuter}.
     *
     * @param manager
     *            The manager requesting the creation
     * @param baseUrl
     *            The URL of the manager
     * @return The new executer
     * @throws IOException
     *             If there is an error creating the executer
     */
    JobExecuter createJobExecuter(JobManager manager, URL baseUrl)
            throws IOException;
}
