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

import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;

/**
 * A factory that produces job parameters.
 */
public abstract class JobParametersFactory {
    /**
     * The argument to append to the script name to request that the system is
     * added to the command line.
     */
    // NB has a space at the start
    public static final String SYSTEM_ARG = " {system}";

    /** The default name of the python script that will be constructed. */
    public static final String DEFAULT_SCRIPT_NAME = "run.py";

    /**
     * Gets job parameters given job description data.
     *
     * @param job
     *            The job to be executed
     * @param workingDirectory
     *            The working directory where the job will be run
     * @param setupScript
     *            The setup script to run
     * @return A job description to be executed
     * @throws UnsupportedJobException
     *             If the factory does not support the job
     * @throws JobParametersFactoryException
     *             If there was an error getting the parameters
     */
    public abstract JobParameters getJobParameters(Job job,
            File workingDirectory, String setupScript)
            throws UnsupportedJobException, JobParametersFactoryException;

    /** The factories for converting jobs into parameters. */
    private static final JobParametersFactory[] JOB_PARAMETER_FACTORIES =
            new JobParametersFactory[]{new GitPyNNJobParametersFactory(),
                new ZipPyNNJobParametersFactory(),
                new DirectPyNNJobParametersFactory()};

    /**
     * Get the parameters from a job.
     *
     * @param job
     *            The job that these will be parameters for.
     * @param workingDirectory
     *            The working directory for the job.
     * @param setupScript
     *            The setup script.
     * @param errors
     *            What errors were found in the process of getting the
     *            parameters.
     * @return The parameters, or {@code null} if the parameters can't be
     *         generated.
     */
    public static JobParameters getJobParameters(final Job job,
            final File workingDirectory, final String setupScript,
            final Map<String, JobParametersFactoryException> errors) {
        for (final var factory : JOB_PARAMETER_FACTORIES) {
            try {
                final var parameters = factory.getJobParameters(
                        job, workingDirectory, setupScript);
                if (parameters != null) {
                    return parameters;
                }
            } catch (final UnsupportedJobException e) {
                // Do Nothing
            } catch (final JobParametersFactoryException e) {
                errors.put(factory.getClass().getSimpleName(), e);
            }
        }
        return null;
    }
}
