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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;

/**
 * A {@link JobParametersFactory} that uses the {@code experimentDescription}
 * itself as a PyNN script.
 */
class DirectPyNNJobParametersFactory extends JobParametersFactory {
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
    * @throws IOException If the file can't be found to write
    */
    private JobParameters constructParameters(final Job job,
            final File workingDirectory, final String setupScript)
            throws IOException {
        final var scriptFile = new File(workingDirectory, DEFAULT_SCRIPT_NAME);
        try (var writer = new PrintWriter(scriptFile, UTF_8)) {
            writer.print(job.getCode());
        }

        return new PyNNJobParameters(workingDirectory, setupScript,
                DEFAULT_SCRIPT_NAME + SYSTEM_ARG, job.getHardwareConfig());
    }
}
