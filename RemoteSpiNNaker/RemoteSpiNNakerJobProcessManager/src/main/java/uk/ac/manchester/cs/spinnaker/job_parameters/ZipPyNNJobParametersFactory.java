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

import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job.pynn.PyNNJobParameters;

/**
 * A {@link JobParametersFactory} that downloads a PyNN job as a zip or tar.gz
 * file. The URL must refer to a world-readable URL or the credentials must be
 * present in the URL.
 */
class ZipPyNNJobParametersFactory extends JobParametersFactory {
    @Override
    public JobParameters getJobParameters(final Job job,
            final File workingDirectory)
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
            return constructParameters(job, workingDirectory, url);
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

    private static final CompressionType[] SUPPORTED_TYPES =
            new CompressionType[]{BZIP2, GZIP};

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

    private JobParameters constructParameters(final Job job,
            final File workingDirectory, final URL url)
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

        return new PyNNJobParameters(workingDirectory.getAbsolutePath(), script,
                job.getHardwareConfig());
    }
}
