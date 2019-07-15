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
package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.io.File;
import java.util.List;

import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.Status;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

/**
 * An interface to an executable job process type.
 *
 * @param <P>
 *            The type of the parameters
 */
public interface JobProcess<P extends JobParameters> {
    /**
     * Executes the job.
     *
     * @param machineUrl
     *            The URL to request a machine using (or <tt>null</tt> if
     *            machine is given)
     * @param machine
     *            The machine to execute the job on (or <tt>null</tt> if a URL
     *            is given)
     * @param parameters
     *            The parameters of the job
     * @param logWriter
     *            Somewhere to write logs to
     */
    void execute(String machineUrl, SpinnakerMachine machine, P parameters,
            LogWriter logWriter);

    /**
     * Gets the status of the job.
     *
     * @return The status
     */
    Status getStatus();

    /**
     * Gets any errors returned by the job. If the status is not Error, this
     * should return <tt>null</tt>
     *
     * @return An error, or <tt>null</tt> if no error
     */
    Throwable getError();

    /**
     * Gets any outputs from the job. Should always return a list, but this list
     * can be empty if there are no outputs.
     *
     * @return A list of output files.
     */
    List<File> getOutputs();

    /**
     * Gets any provenance data from the job. Should always return a list, but
     * this map can be empty if there is no provenance.
     *
     * @return A list of provenance items to add
     */
    List<ProvenanceItem> getProvenance();

    /**
     * Cleans up the job, removing any associated files.
     */
    void cleanup();
}
