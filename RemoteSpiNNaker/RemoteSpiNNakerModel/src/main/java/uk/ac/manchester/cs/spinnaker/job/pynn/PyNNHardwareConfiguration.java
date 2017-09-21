package uk.ac.manchester.cs.spinnaker.job.pynn;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class PyNNHardwareConfiguration {

    private String pyNNVersion = null;

    private String softwareVersion = null;

    private Map<String, Object> others = new HashMap<>();

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

    @JsonAnySetter
    public void addOther(String key, Object value) {
        others.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOthers() {
        return others;
    }

}
