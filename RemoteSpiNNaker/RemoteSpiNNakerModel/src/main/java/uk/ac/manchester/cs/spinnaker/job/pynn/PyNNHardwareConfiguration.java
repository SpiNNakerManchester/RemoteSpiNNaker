package uk.ac.manchester.cs.spinnaker.job.pynn;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class PyNNHardwareConfiguration {

    public static final String PYNN_0_8 = "0.8";

    public static final String PYNN_0_7 = "0.7";

    public static final String PYNN_VERSION_KEY = "pynn_version";

    public static final String SOFTWARE_VERSION_KEY = "spynnaker_version";

    public static final String LOW_LEVEL_SOFTWARE_VERSION_KEY =
        "spinnaker_tools_version";

    public static final String GIT_BASE_TAG_KEY = "git_base_tag";

    public static final String MAKE_DIRS_KEY = "make_dirs";

    public static final String PYTHON_SETUP_DIRS_KEY = "setup_dirs";

    private static final String DEFAULT_PYNN_VERSION = PYNN_0_8;

    private static final String DEFAULT_SOFTWARE_VERSION = "4.0.0";

    private static final String DEFAULT_LOW_LEVEL_SOFTWARE_VERSION = "3.1.1";

    private static final String DEFAULT_GIT_BASE_TAG = "master";

    private static final String DEFAULT_MAKE_DIRS = "";

    private static final String DEFAULT_PYTHON_SETUP_DIRS = "";

    private String pyNNVersion = null;

    private String softwareVersion = null;

    private String lowLevelSoftwareVersion = null;

    private String[] makeDirs = null;

    private String[] pythonSetupDirs = null;

    private Map<String, Object> others = new HashMap<>();

    public PyNNHardwareConfiguration() {
        // Does Nothing
    }

    @SuppressWarnings("unchecked")
    private <T> T getValue(
            String key, Map<String, Object> jobHardwareConfiguration,
            T defaultValue, Class<T> type) {
        if (jobHardwareConfiguration.containsKey(key)) {
            Object value = jobHardwareConfiguration.get(key);
            if (!type.isInstance(value)) {
                throw new RuntimeException(
                    "Expected a String in hardware configuration for "
                    + key + " but got " + value.getClass() + " instead");
            }
            return (T) value;
        }
        return defaultValue;
    }

    public PyNNHardwareConfiguration(
            Map<String, Object> jobHardwareConfiguration) {
        pyNNVersion = getValue(
            PYNN_VERSION_KEY, jobHardwareConfiguration, DEFAULT_PYNN_VERSION,
            String.class);
        softwareVersion = getValue(
            SOFTWARE_VERSION_KEY, jobHardwareConfiguration,
            DEFAULT_SOFTWARE_VERSION, String.class);

        String makeDirsString = getValue(
            MAKE_DIRS_KEY, jobHardwareConfiguration, DEFAULT_MAKE_DIRS,
            String.class);
        makeDirs = makeDirsString.replace(" ", "").split(";");

        String pythonSetupDirsString = getValue(
            PYTHON_SETUP_DIRS_KEY, jobHardwareConfiguration,
            DEFAULT_PYTHON_SETUP_DIRS, String.class);
        pythonSetupDirs = pythonSetupDirsString.replace(" ", "").split(";");
    }

    public String getPyNNVersion() {
        return pyNNVersion;
    }

    public void setPyNNVersion(String pyNNVersion) {
        this.pyNNVersion = pyNNVersion;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String[] getMakeDirs() {
        return makeDirs;
    }

    public void setMakeDirs(String[] makeDirs) {
        this.makeDirs = makeDirs;
    }

    public String[] getPythonSetupDirs() {
        return pythonSetupDirs;
    }

    public void setPythonSetupDirs(String[] pythonSetupDirs) {
        this.pythonSetupDirs = pythonSetupDirs;
    }

    @JsonAnySetter
    public void addOther(String key, Object value) {
        others.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOthers() {
        return others;
    }

}
