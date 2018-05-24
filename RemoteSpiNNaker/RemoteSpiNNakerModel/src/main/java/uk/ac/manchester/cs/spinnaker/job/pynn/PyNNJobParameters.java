package uk.ac.manchester.cs.spinnaker.job.pynn;

import java.util.Map;

import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.JobParametersTypeName;

/**
 * Represents the parameters required for a PyNN job.
 */
@JobParametersTypeName("PyNNJobParameters")
public class PyNNJobParameters implements JobParameters {
    private String workingDirectory;
    private String script;
    private Map<String, Object> hardwareConfiguration;

    public PyNNJobParameters() {
        // Does Nothing
    }

    /**
     * Create a description of the job parameters for a PyNN job.
     *
     * @param workingDirectory
     *            The working directory to use.
     * @param script
     *            The script to run.
     * @param hardwareConfiguration
     *            The hardware configuration desired.
     */
    public PyNNJobParameters(final String workingDirectory, final String script,
            final Map<String, Object> hardwareConfiguration) {
        this.workingDirectory = workingDirectory;
        this.script = script;
        this.hardwareConfiguration = hardwareConfiguration;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(final String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getScript() {
        return script;
    }

    public void setScript(final String script) {
        this.script = script;
    }

    public Map<String, Object> getHardwareConfiguration() {
        return hardwareConfiguration;
    }

    public void setHardwareConfiguration(
            final Map<String, Object> hardwareConfiguration) {
        this.hardwareConfiguration = hardwareConfiguration;
    }
}
