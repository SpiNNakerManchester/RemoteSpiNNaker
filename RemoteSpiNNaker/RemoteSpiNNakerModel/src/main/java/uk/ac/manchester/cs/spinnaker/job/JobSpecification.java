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

import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

/**
 * The specification for a job. Can be any type of job, though the rest of this
 * implementation only really supports PyNN jobs running on SpiNNaker hardware
 * using sPyNNaker.
 */
public class JobSpecification {

    /**
     * The machine to execute the job on.
     */
    private SpinnakerMachine machine;

    /**
     * The parameters of the job.
     */
    private JobParameters parameters;

    /**
     * The ID of the job.
     */
    private int id;

    /**
     * The URL of the job to send results and status to.
     */
    private String url;

    /**
     * Constructor for serialisation.
     */
    public JobSpecification() {
        // Does Nothing
    }

    /**
     * Create the specification for running a job.
     *
     * @param machineParam
     *            The machine to run the job on.
     * @param parametersParam
     *            The parameters to the job.
     * @param idParam
     *            The ID of the job.
     * @param urlParam
     *            The URL
     */
    public JobSpecification(final SpinnakerMachine machineParam,
            final JobParameters parametersParam, final int idParam,
            final String urlParam) {
        this.machine = machineParam;
        this.parameters = parametersParam;
        this.id = idParam;
        this.url = urlParam;
    }

    /**
     * Get the machine.
     *
     * @return the machine
     */
    public SpinnakerMachine getMachine() {
        return machine;
    }

    /**
     * Sets the machine.
     *
     * @param machineParam the machine to set
     */
    public void setMachine(final SpinnakerMachine machineParam) {
        this.machine = machineParam;
    }

    /**
     * Get the parameters.
     *
     * @return the parameters
     */
    public JobParameters getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters.
     *
     * @param parametersParam the parameters to set
     */
    public void setParameters(final JobParameters parametersParam) {
        this.parameters = parametersParam;
    }

    /**
     * Get the id.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param idParam the id to set
     */
    public void setId(final int idParam) {
        this.id = idParam;
    }

    /**
     * Get the URL.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL.
     *
     * @param urlParam the URL to set
     */
    public void setUrl(final String urlParam) {
        this.url = urlParam;
    }
}
