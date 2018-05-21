package uk.ac.manchester.cs.spinnaker.job.nmpi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A NMPI job.
 */
public class Job implements QueueNextResponse {
    private String code;
    private Map<String, Object> hardwareConfig;
    private String hardwarePlatform;
    private Integer id;
    private List<DataItem> inputData;
    private List<DataItem> outputData;
    private String collabId;
    private String resourceUri;
    private String status;
    private String command;
    private String userId;
    private long resourceUsage;
    @JsonSerialize(using = DateTimeSerialiser.class)
    private DateTime timestampCompletion;
    @JsonSerialize(using = DateTimeSerialiser.class)
    private DateTime timestampSubmission;
    private ObjectNode provenance;
    private Map<String, Object> others = new HashMap<String, Object>();

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public Map<String, Object> getHardwareConfig() {
        return hardwareConfig;
    }

    public void setHardwareConfig(final Map<String, Object> hardwareConfig) {
        this.hardwareConfig = hardwareConfig;
    }

    public String getHardwarePlatform() {
        return hardwarePlatform;
    }

    public void setHardwarePlatform(final String hardwarePlatform) {
        this.hardwarePlatform = hardwarePlatform;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public List<DataItem> getInputData() {
        return inputData;
    }

    public void setInputData(final List<DataItem> inputData) {
        this.inputData = inputData;
    }

    public List<DataItem> getOutputData() {
        return outputData;
    }

    public void setOutputData(final List<DataItem> outputData) {
        this.outputData = outputData;
    }

    public String getCollabId() {
        return collabId;
    }

    public void setCollabId(final String collabId) {
        this.collabId = collabId;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(final String resourceUri) {
        this.resourceUri = resourceUri;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public DateTime getTimestampCompletion() {
        return timestampCompletion;
    }

    public void setTimestampCompletion(final DateTime timestampCompletion) {
        this.timestampCompletion = timestampCompletion;
    }

    public DateTime getTimestampSubmission() {
        return timestampSubmission;
    }

    public void setTimestampSubmission(final DateTime timestampSubmission) {
        this.timestampSubmission = timestampSubmission;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(final String command) {
        this.command = command;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public long getResourceUsage() {
        return resourceUsage;
    }

    public void setResourceUsage(final long resourceUsage) {
        this.resourceUsage = resourceUsage;
    }

    public ObjectNode getProvenance() {
        return provenance;
    }

    public void setProvenance(final ObjectNode provenance) {
        this.provenance = provenance;
    }

    @JsonAnySetter
    public void set(final String name, final Object value) {
        System.err.println("Warning: Job contains unexpected item " + name);
        others.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOthers() {
        return others;
    }
}
