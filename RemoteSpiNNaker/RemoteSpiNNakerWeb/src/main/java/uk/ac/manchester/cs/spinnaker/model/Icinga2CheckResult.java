package uk.ac.manchester.cs.spinnaker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Icinga2CheckResult {

    /**
     * The exit status of the service or host.
     * For services, 0=OK, 1=WARNING, 2=CRITICAL, 3=UNKNOWN.
     * For hosts, 0=OK, 1=CRITICAL
     */
    @JsonProperty("exit_status")
    private int exitStatus;

    /**
     * An output string to report.
     */
    @JsonProperty("plugin_output")
    private String pluginOutput;

    /**
     * Optional performance data to report.
     */
    @JsonProperty("performance_data")
    private String performanceData;

    /**
     * Optional durations in seconds of the test result.
     */
    private Integer ttl;

    /**
     * The target host being reported on.
     */
    private String host;

    /**
     * Optional target service being reported on.
     */
    private String service;

    public Icinga2CheckResult(int exitStatus, String pluginOutput, String performanceData,
            Integer ttl, String host, String service) {
        this.exitStatus = exitStatus;
        this.pluginOutput = pluginOutput;
        this.performanceData = performanceData;
        this.ttl = ttl;
        this.host = host;
        this.service = service;
    }
}
