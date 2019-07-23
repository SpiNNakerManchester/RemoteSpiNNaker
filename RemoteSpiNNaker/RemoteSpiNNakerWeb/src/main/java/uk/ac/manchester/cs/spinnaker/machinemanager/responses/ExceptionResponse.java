/*
 * Copyright (c) 2014-2019 The University of Manchester
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
package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A response to a request that indicates a failure.
 */
public class ExceptionResponse implements Response {

    /**
     * The exception to report.
     */
    private String exception;

    /**
     * Get the exception to report.
     *
     * @return The exception
     */
    public String getException() {
        return exception;
    }

    /**
     * Set the exception to report.
     *
     * @param exceptionParam The exception to set
     */
    @JsonSetter("exception")
    public void setException(final JsonNode exceptionParam) {
        this.exception = exceptionParam.toString();
    }
}
