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
package uk.ac.manchester.cs.spinnaker.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import uk.ac.manchester.cs.spinnaker.model.Icinga2CheckResult;

/**
 * Interface to the Icinga2 API for status monitoring.
 */
public interface Icinga2 {

    /**
     * Update the status of a service or host.
     *
     * @param result The result of a status check to update with.
     * @return The response from the server as a String.
     */
    @Produces("application/json")
    @Consumes("application/json")
    @POST
    @Path("/v1/actions/process-check-result")
    String processCheckResult(Icinga2CheckResult result);
}
