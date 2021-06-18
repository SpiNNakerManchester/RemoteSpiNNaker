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
package uk.ac.manchester.cs.spinnaker.job_parameters;

import java.io.File;
import java.util.Map;

/**
 * Represents the parameters required for a PyNN job.
 */
@JobParametersTypeName("PyNNJobParameters")
public class PyNNJobParameters implements JobParameters {

    /**
     * The directory in which the job should be run.
     */
    private File workingDirectory;

    /**
     * The system (bash) script to be executed to setup the environment.
     */
    private String setupScript;

    /**
     * The user (python) script to eventually execute.
     */
    private String userScript;

    /**
     * The configuration of the hardware.
     */
    private Map<String, Object> hardwareConfiguration;

    /**
     * Create an empty parameters for serialisation.
     */
    public PyNNJobParameters() {
        // Does Nothing
    }

    /**
     * Create a description of the job parameters for a PyNN job.
     *
     * @param workingDirectoryParam
     *            The working directory to use.
     * @param setupScriptParam
     *            The setup script to run before execution
     * @param userScriptParam
     *            The user script to run.
     * @param hardwareConfigurationParam
     *            The hardware configuration desired.
     */
    public PyNNJobParameters(final File workingDirectoryParam,
            final String setupScriptParam, final String userScriptParam,
            final Map<String, Object> hardwareConfigurationParam) {
        this.workingDirectory = workingDirectoryParam;
        this.userScript = userScriptParam;
        this.setupScript = setupScriptParam;
        this.hardwareConfiguration = hardwareConfigurationParam;
    }

    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Get the setup script.
     *
     * @return the script
     */
    public String getSetupScript() {
        return setupScript;
    }

    /**
     * Get the user script.
     *
     * @return the script
     */
    public String getUserScript() {
        return userScript;
    }

    /**
     * Get the hardwareConfiguration.
     *
     * @return the hardwareConfiguration
     */
    public Map<String, Object> getHardwareConfiguration() {
        return hardwareConfiguration;
    }
}
