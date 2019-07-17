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

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

/**
 * An interface to the UNICORE storage REST API.
 */
@Path("/storages")
public interface UnicoreFileClient {
    /**
     * Upload a file.
     *
     * @param id
     *            The id of the storage on the server.
     * @param filePath
     *            The path at which to store the file (directories are
     *            automatically created).
     * @param input
     *            The input stream containing the file to upload.
     * @throws WebApplicationException
     *             If anything goes wrong.
     */
    @PUT
    @Path("{id}/files/{filePath}")
    @Consumes("application/octet-stream")
    void upload(@PathParam("id") String id,
            @PathParam("filePath") String filePath, InputStream input)
            throws WebApplicationException;
}
