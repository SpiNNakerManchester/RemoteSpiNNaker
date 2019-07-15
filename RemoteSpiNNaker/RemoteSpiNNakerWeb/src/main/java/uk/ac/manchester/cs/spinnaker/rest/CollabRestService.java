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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import uk.ac.manchester.cs.spinnaker.model.CollabContext;
import uk.ac.manchester.cs.spinnaker.model.CollabPermissions;

/**
 * The REST API for the collabratory.
 */
@Path("/collab/v0")
public interface CollabRestService {
    /**
     * Get the context token.
     *
     * @param contextId
     *            The collabratory ID.
     * @return the token.
     */
    @GET
    @Path("/collab/context/{contextId}")
    @Produces(APPLICATION_JSON)
    CollabContext getCollabContext(@PathParam("contextId") String contextId);

    /**
     * Get the context permissions.
     *
     * @param id
     *            The collabratory ID.
     * @return The permissions set
     */
    @GET
    @Path("/collab/{id}/permissions")
    @Produces(APPLICATION_JSON)
    CollabPermissions getCollabPermissions(@PathParam("id") int id);
}
