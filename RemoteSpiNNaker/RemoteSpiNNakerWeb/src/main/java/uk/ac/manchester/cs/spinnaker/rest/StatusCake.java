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
package uk.ac.manchester.cs.spinnaker.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * Interface to StatusCake API.
 *
 */
public interface StatusCake {

    /**
     * Send a push update.
     *
     * @param primaryKey The key of the update.
     * @param testID The ID of the test within the set.
     * @param time The "time" or any performance of the status.
     */
    @GET
    @Path("")
    void pushUpdate(@QueryParam("PK") String primaryKey,
            @QueryParam("TestID") String testID,
            @QueryParam("time") int time);

}
