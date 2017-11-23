package uk.ac.manchester.cs.spinnaker.job.pynn;

import java.util.Map;

import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.JobParametersTypeName;

/**
 * Represents the parameters required for a PyNN job.
 */
@JobParametersTypeName("PyNNJobParameters")
public class PyNNJobParameters implements JobParameters {

    /**
     * The directory in which the job should be run.
     */
    private String workingDirectory;

    /**
     * The script to execute.
     */
    private String script;

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
     * Create a normal parameters object.
     *
     * @param workingDirectoryParam The directory in which to execute the job
     * @param scriptParam The script to execute
     * @param hardwareConfigurationParam The hardware configuration
     */
    public PyNNJobParameters(final String workingDirectoryParam,
            final String scriptParam,
            final Map<String, Object> hardwareConfigurationParam) {
        this.workingDirectory = workingDirectoryParam;
        this.script = scriptParam;
        this.hardwareConfiguration = hardwareConfigurationParam;
    }

    /**
     * Get the workingDirectory.
     *
     * @return the workingDirectory
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the workingDirectory.
     *
     * @param workingDirectoryParam the workingDirectory to set
     */
    public void setWorkingDirectory(final String workingDirectoryParam) {
        this.workingDirectory = workingDirectoryParam;
    }

    /**
     * Get the script.
     *
     * @return the script
     */
    public String getScript() {
        return script;
    }

    /**
     * Sets the script.
     *
     * @param scriptParam the script to set
     */
    public void setScript(final String scriptParam) {
        this.script = scriptParam;
    }

    /**
     * Get the hardwareConfiguration.
     *
     * @return the hardwareConfiguration
     */
    public Map<String, Object> getHardwareConfiguration() {
        return hardwareConfiguration;
    }

    /**
     * Sets the hardwareConfiguration.
     *
     * @param hardwareConfigurationParam the hardwareConfiguration to set
     */
    public void setHardwareConfiguration(
            final Map<String, Object> hardwareConfigurationParam) {
        this.hardwareConfiguration = hardwareConfigurationParam;
    }
}
