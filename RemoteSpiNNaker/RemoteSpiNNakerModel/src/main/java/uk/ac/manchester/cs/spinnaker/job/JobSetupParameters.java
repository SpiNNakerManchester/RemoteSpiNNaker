package uk.ac.manchester.cs.spinnaker.job;

import java.util.List;
import java.util.Map;

/**
 * A set of parameters used to set up the job depending on the configuration.
 *
 */
public class JobSetupParameters {

    /**
     * A map of configuration name to git repositories to clone.
     */
    private Map<String, List<String>> gitRepositories;

    /**
     * A map of configuration name to directories to call "make" in.
     */
    private Map<String, List<String>> makeDirs;

    /**
     * A map of configuration name to directories to call "python setup.py" in.
     */
    private Map<String, List<String>> pythonSetupDirs;

    /**
     * Get the gitRepositories.
     *
     * @return the gitRepositories
     */
    public Map<String, List<String>> getGitRepositories() {
        return gitRepositories;
    }

    /**
     * Sets the gitRepositories.
     *
     * @param theGitRepositories the gitRepositories to set
     */
    public void setGitRepositories(
            final Map<String, List<String>> theGitRepositories) {
        this.gitRepositories = theGitRepositories;
    }

    /**
     * Get the makeDirs.
     *
     * @return the makeDirs
     */
    public Map<String, List<String>> getMakeDirs() {
        return makeDirs;
    }

    /**
     * Sets the makeDirs.
     *
     * @param theMakeDirs the makeDirs to set
     */
    public void setMakeDirs(final Map<String, List<String>> theMakeDirs) {
        this.makeDirs = theMakeDirs;
    }

    /**
     * Get the pythonSetupDirs.
     *
     * @return the pythonSetupDirs
     */
    public Map<String, List<String>> getPythonSetupDirs() {
        return pythonSetupDirs;
    }

    /**
     * Sets the pythonSetupDirs.
     *
     * @param thePythonSetupDirs the pythonSetupDirs to set
     */
    public void setPythonSetupDirs(
            final Map<String, List<String>> thePythonSetupDirs) {
        this.pythonSetupDirs = thePythonSetupDirs;
    }


}
