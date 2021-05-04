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

import static org.rauschig.jarchivelib.ArchiverFactory.createArchiver;
import static org.rauschig.jarchivelib.CompressionType.BZIP2;
import static org.rauschig.jarchivelib.CompressionType.GZIP;
import static uk.ac.manchester.cs.spinnaker.utils.FileDownloader.downloadFile;
import static uk.ac.manchester.cs.spinnaker.utils.Log.log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.CompressionType;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;

/**
 * A {@link JobParametersFactory} that downloads a PyNN job as a zip or tar.gz
 * file. The URL must refer to a world-readable URL or the credentials must be
 * present in the URL.
 */
class ZipPyNNJobParametersFactory extends JobParametersFactory {
    @Override
    public JobParameters getJobParameters(final Job job,
            final File workingDirectory, final String setupScript)
            throws UnsupportedJobException, JobParametersFactoryException {
        // Test that there is a URL
        final String jobCodeLocation = job.getCode().trim();
        if (!jobCodeLocation.startsWith("http://")
                && !jobCodeLocation.startsWith("https://")) {
            throw new UnsupportedJobException();
        }

        // Test that the URL is well formed
        URL url;
        try {
            url = new URL(jobCodeLocation);
        } catch (final MalformedURLException e) {
            throw new JobParametersFactoryException("The URL is malformed", e);
        }

        // Try to get the file and extract it
        try {
            return constructParameters(job, workingDirectory, url, setupScript);
        } catch (final IOException e) {
            log(e);
            throw new JobParametersFactoryException(
                    "Error in communication or extraction", e);
        } catch (final Throwable e) {
            log(e);
            throw new JobParametersFactoryException(
                    "General error with zip extraction", e);
        }
    }

    /**
     * The supported compression types.
     */
    private static final CompressionType[] SUPPORTED_TYPES =
            new CompressionType[]{BZIP2, GZIP};

    /**
     * Extract an archive using auto-detection for the format.
     *
     * @param output The archive to extract
     * @param workingDirectory The directory to extract into
     * @return True if extracted, False if failed
     * @throws IOException If there is a general error in extraction
     */
    private boolean extractAutodetectedArchive(final File output,
            final File workingDirectory) throws IOException {
        try {
            final Archiver archiver = createArchiver(output);
            archiver.extract(output, workingDirectory);
            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extract an archive by trying known archive types.
     *
     * @param workingDirectory The directory to extract into
     * @param output The archive to extract
     * @return True if the archive was extracted, False otherwise
     */
    private boolean extractArchiveUsingKnownFormats(final File workingDirectory,
            final File output) {
        for (final ArchiveFormat format : ArchiveFormat.values()) {
            try {
                final Archiver archiver = createArchiver(format);
                archiver.extract(output, workingDirectory);
                return true;
            } catch (final IOException e) {
                // Ignore - try the next
            }
        }
        return false;
    }

    /**
     * Extract an archive by trying internal list of archive types.
     *
     * @param workingDirectory The directory to extract into
     * @param output The archive to extract
     * @return True if the archive was extracted, False otherwise
     */
    private boolean extractTypedArchive(final File workingDirectory,
            final File output) {
        for (final ArchiveFormat format : ArchiveFormat.values()) {
            for (final CompressionType type : SUPPORTED_TYPES) {
                try {
                    final Archiver archiver = createArchiver(format, type);
                    archiver.extract(output, workingDirectory);
                    return true;
                } catch (final IOException e) {
                    // Ignore - try the next
                }
            }
        }
        return false;
    }

    /**
     * Build the job parameters.
     *
     * @param job The job to build the parameters for
     * @param workingDirectory The directory where the job should be run
     * @param url The URL of the archive to use
     * @param setupScript The setup script
     * @return The constructed parameters
     * @throws IOException If there is an error with the file
     * @throws JobParametersFactoryException If no way to uncompress the file
     *     could be found
     */
    private JobParameters constructParameters(final Job job,
            final File workingDirectory, final URL url,
            final String setupScript)
            throws IOException, JobParametersFactoryException {
        final File output = downloadFile(url, workingDirectory, null);

        /* Test if there is a recognised archive */
        boolean archiveExtracted =
                extractAutodetectedArchive(output, workingDirectory);

        /*
         * If the archive wasn't extracted by the last line, try the known
         * formats
         */
        if (!archiveExtracted) {
            archiveExtracted =
                    extractArchiveUsingKnownFormats(workingDirectory, output);
        }

        /*
         * If the archive was still not extracted, try again with different
         * compression types
         */
        if (!archiveExtracted) {
            archiveExtracted = extractTypedArchive(workingDirectory, output);
        }

        // Delete the archive
        if (!output.delete()) {
            log("Warning, could not delete file " + output);
        }

        // If the archive wasn't extracted, throw an error
        if (!archiveExtracted) {
            throw new JobParametersFactoryException(
                    "The URL could not be decompressed with any known method");
        }

        String script = DEFAULT_SCRIPT_NAME + SYSTEM_ARG;
        final String command = job.getCommand();
        if ((command != null) && !command.isEmpty()) {
            script = command;
        }

        return new PyNNJobParameters(workingDirectory.getAbsolutePath(),
                setupScript, script, job.getHardwareConfig());
    }
}
