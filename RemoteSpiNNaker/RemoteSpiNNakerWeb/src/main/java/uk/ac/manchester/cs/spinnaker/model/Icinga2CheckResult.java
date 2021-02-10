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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A result to report to Icinga.
 *
 */
@JsonInclude(Include.NON_NULL)
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
    @JsonProperty("ttl")
    private Integer ttl;

    /**
     * The target host being reported on.
     */
    @JsonProperty("host")
    private String host;

    /**
     * Optional target service being reported on.
     */
    @JsonProperty("service")
    private String service;

    /**
     * Create a new result to report.
     *
     * @param exitStatusParam The status to report.
     * @param pluginOutputParam An output string to add to the report.
     * @param performanceDataParam Any performance data to report as a string.
     * @param ttlParam The time at which the next report is expected in seconds.
     * @param hostParam The host to report on.
     * @param serviceParam The service to report on.
     */
    public Icinga2CheckResult(final int exitStatusParam,
            final String pluginOutputParam, final String performanceDataParam,
            final Integer ttlParam, final String hostParam,
            final String serviceParam) {
        exitStatus = exitStatusParam;
        pluginOutput = pluginOutputParam;
        performanceData = performanceDataParam;
        ttl = ttlParam;
        host = hostParam;
        service = serviceParam;
    }
}
