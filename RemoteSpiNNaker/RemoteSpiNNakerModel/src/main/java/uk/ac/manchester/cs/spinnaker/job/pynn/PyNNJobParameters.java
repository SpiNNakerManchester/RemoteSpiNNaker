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
     * The system (bash) script to be executed to setup the environment.
     */
    private String setupScript;

    /**
     * The user (python) script to eventually execute.
     */
    private String userScript;

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
     * Create a description of the job parameters for a PyNN job.
     *
     * @param workingDirectoryParam
     *            The working directory to use.
     * @param setupScriptParam
     *            The setup script to run before execution
     * @param userScriptParam
     *            The user script to run.
     * @param hardwareConfigurationParam
     *            The hardware configuration desired.
     */
    public PyNNJobParameters(final String workingDirectoryParam,
            final String setupScriptParam, final String userScriptParam,
            final Map<String, Object> hardwareConfigurationParam) {
        this.workingDirectory = workingDirectoryParam;
        this.userScript = userScriptParam;
        this.setupScript = setupScriptParam;
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
     * Get the setup script.
     *
     * @return the script
     */
    public String getSetupScript() {
        return setupScript;
    }

    /**
     * Set the setup script.
     *
     * @param setupScriptParam the script
     */
    public void setSetupScript(final String setupScriptParam) {
        this.setupScript = setupScriptParam;
    }

    /**
     * Get the user script.
     *
     * @return the script
     */
    public String getUserScript() {
        return userScript;
    }

    /**
     * Sets the user script.
     *
     * @param userScriptParam the script to set
     */
    public void setUserScript(final String userScriptParam) {
        this.userScript = userScriptParam;
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
