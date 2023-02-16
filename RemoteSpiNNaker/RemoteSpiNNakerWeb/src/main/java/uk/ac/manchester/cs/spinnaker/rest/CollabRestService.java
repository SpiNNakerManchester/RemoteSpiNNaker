/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
