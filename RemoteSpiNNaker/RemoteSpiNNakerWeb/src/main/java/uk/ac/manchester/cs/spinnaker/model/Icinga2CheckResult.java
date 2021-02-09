/*
 * Copyright (c) 2020 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.cs.spinnaker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A result to report to Icinga.
 *
 */
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

    /**
     * Create a new result to report.
     *
     * @param exitStatus The status to report.
     * @param pluginOutput An output string to add to the report.
     * @param performanceData Any performance data to report as a string.
     * @param ttl The time at which the next report is expected in seconds.
     * @param host The host to report on.
     * @param service The service to report on.
     */
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
