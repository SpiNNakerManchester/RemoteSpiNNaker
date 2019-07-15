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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job.pynn.PyNNJobParameters;

/**
 * A {@link JobParametersFactory} that uses the {@code experimentDescription}
 * itself as a PyNN script.
 */
class DirectPyNNJobParametersFactory extends JobParametersFactory {

    /**
     * Encoding of the output script.
     */
    private static final String ENCODING = "UTF-8";

    @Override
    public JobParameters getJobParameters(final Job job,
            final File workingDirectory, final String setupScript)
            throws UnsupportedJobException, JobParametersFactoryException {
        if (!job.getCode().contains("import")) {
            throw new UnsupportedJobException();
        }

        try {
            return constructParameters(job, workingDirectory, setupScript);
        } catch (final IOException e) {
            throw new JobParametersFactoryException("Error storing script", e);
        } catch (final Throwable e) {
            throw new JobParametersFactoryException(
                    "General error with PyNN Script", e);
        }
    }

   /**
    * Constructs the parameters by writing the script into a local file.
    *
    * @param job The job to construct parameters for
    * @param workingDirectory The directory where the job should be started
    * @param setupScript The setup script to run
    * @return The parameters created
    * @throws FileNotFoundException If the file can't be found to write
    * @throws UnsupportedEncodingException If the encoding failed (unlikely)
    */
    private JobParameters constructParameters(final Job job,
            final File workingDirectory, final String setupScript)
            throws FileNotFoundException, UnsupportedEncodingException {
        final File scriptFile = new File(workingDirectory, DEFAULT_SCRIPT_NAME);
        try (PrintWriter writer = new PrintWriter(scriptFile, ENCODING)) {
            writer.print(job.getCode());
        }

        return new PyNNJobParameters(workingDirectory.getAbsolutePath(),
                setupScript, DEFAULT_SCRIPT_NAME + SYSTEM_ARG,
                job.getHardwareConfig());
    }
}
