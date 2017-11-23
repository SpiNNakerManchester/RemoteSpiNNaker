package uk.ac.manchester.cs.spinnaker.job_parameters;

import static org.eclipse.jgit.api.Git.cloneRepository;

import java.io.File;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job.pynn.PyNNHardwareConfiguration;
import uk.ac.manchester.cs.spinnaker.job.pynn.PyNNJobParameters;

/**
 * A {@link JobParametersFactory} that downloads a PyNN job from git. The git
 * repository must be world-readable, or sufficient credentials must be present
 * in the URL.
 */
class GitPyNNJobParametersFactory extends JobParametersFactory {
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

        // Try to get the repository
        try {
            return constructParameters(job, workingDirectory, jobCodeLocation);
        } catch (final InvalidRemoteException e) {
            throw new JobParametersFactoryException("Remote is not valid", e);
        } catch (final TransportException e) {
            throw new JobParametersFactoryException("Transport failed", e);
        } catch (final GitAPIException e) {
            throw new JobParametersFactoryException("Error using Git", e);
        } catch (final Throwable e) {
            throw new JobParametersFactoryException(
                    "General error getting git repository", e);
        }
    }

    /** Constructs the parameters by checking out the git repository. */
    private JobParameters constructParameters(final Job job,
            final File workingDirectory, final String experimentDescription)
            throws GitAPIException, InvalidRemoteException, TransportException {
        final CloneCommand clone = cloneRepository();
        clone.setURI(experimentDescription);
        clone.setDirectory(workingDirectory);
        clone.setCloneSubmodules(true);
        clone.call();

        String script = DEFAULT_SCRIPT_NAME + SYSTEM_ARG;
        final String command = job.getCommand();
        if ((command != null) && !command.isEmpty()) {
            script = command;
        }

        return new PyNNJobParameters(workingDirectory.getAbsolutePath(), script,
                new PyNNHardwareConfiguration(job.getHardwareConfig()));
    }
}
