package uk.ac.manchester.cs.spinnaker.job.pynn;

import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.JobParametersTypeName;

/**
 * Represents the parameters required for a PyNN job.
 */
@JobParametersTypeName("PyNNJobParameters")
public class PyNNJobParameters implements JobParameters {
    private String workingDirectory;
    private String script;
    private PyNNHardwareConfiguration hardwareConfiguration;

    public PyNNJobParameters() {
        // Does Nothing
    }

    public PyNNJobParameters(final String workingDirectory, final String script,
            final PyNNHardwareConfiguration hardwareConfiguration) {
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

    public PyNNHardwareConfiguration getHardwareConfiguration() {
        return hardwareConfiguration;
    }

    public void setHardwareConfiguration(
            final PyNNHardwareConfiguration hardwareConfiguration) {
        this.hardwareConfiguration = hardwareConfiguration;
    }
}
