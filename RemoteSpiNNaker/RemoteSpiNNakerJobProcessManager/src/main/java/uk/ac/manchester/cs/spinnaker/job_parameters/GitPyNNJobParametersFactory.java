package uk.ac.manchester.cs.spinnaker.job_parameters;

import static org.eclipse.jgit.api.Git.cloneRepository;

import java.io.File;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
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

    /**
     * Constructs the parameters by checking out the git repository.
     *
     * @param job The job to get the parameters of
     * @param workingDirectory The directory where the job should be run
     * @param experimentDescription The git URL
     * @return The constructed parameters
     * @throws GitAPIException If there is a general problem using Git
     * @throws InvalidRemoteException If there is a problem with the repository
     * @throws TransportException If there is a problem in communication
     * @throws URISyntaxException If the URI syntax is incorrect
     */
    private JobParameters constructParameters(final Job job,
            final File workingDirectory, final String experimentDescription)
            throws GitAPIException, InvalidRemoteException, TransportException,
            URISyntaxException {
        final CloneCommand clone = cloneRepository();
        URIish urish = new URIish(experimentDescription);
        if (urish.getUser() != null) {
            String pass = urish.getPass();
            if (pass == null) {
                pass = "";
            }
            clone.setCredentialsProvider(
                new UsernamePasswordCredentialsProvider(urish.getUser(), pass));
        }
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
                job.getHardwareConfig());
    }
}
