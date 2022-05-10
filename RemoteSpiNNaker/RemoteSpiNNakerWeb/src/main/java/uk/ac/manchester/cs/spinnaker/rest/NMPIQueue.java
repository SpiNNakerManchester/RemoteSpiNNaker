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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job.nmpi.QueueEmpty;
import uk.ac.manchester.cs.spinnaker.job.nmpi.QueueNextResponse;
import uk.ac.manchester.cs.spinnaker.model.APIKeyResponse;
import uk.ac.manchester.cs.spinnaker.model.NMPILog;
import uk.ac.manchester.cs.spinnaker.rest.utils.CustomJacksonJsonProvider;
import uk.ac.manchester.cs.spinnaker.rest.utils.PropertyBasedDeserialiser;

/**
 * The REST API for the HBP Neuromorphic Platform Interface queue.
 */
@Path("/api/v2")
public interface NMPIQueue {
    /**
     * Get the API token.
     *
     * @param username
     *            The username.
     * @return The token.
     */
    @GET
    @Path("token/auth")
    @Produces("application/json")
    APIKeyResponse getToken(@QueryParam("username") String username);

    /**
     * Get the next queue item for a specific hardware system.
     *
     * @param hardware
     *            The hardware ID.
     * @return The queue item.
     */
    @GET
    @Path("queue/submitted/next/{hardware}/")
    @Produces("application/json")
    QueueNextResponse getNextJob(@PathParam("hardware") String hardware);

    /**
     * Update the status of a queue item.
     *
     * @param id
     *            The queue ID
     * @param job
     *            the Job document.
     */
    @PUT
    @Path("queue/{id}")
    @Consumes("application/json")
    void updateJob(@PathParam("id") int id, Job job);

    /**
     * Get the queue status.
     *
     * @param id
     *            The queue ID
     * @return The job on the queue.
     */
    @GET
    @Path("queue/{id}")
    @Produces("application/json")
    Job getJob(@PathParam("id") int id);

    /**
     * Update the log.
     *
     * @param id
     *            The queue ID
     * @param log
     *            The log entry
     */
    @PUT
    @Path("log/{id}")
    @Consumes("application/json")
    void updateLog(@PathParam("id") int id, NMPILog log);

    /**
     * Create a JSON provider capable of handling the messages on the NMPI
     * queue.
     *
     * @return The provider.
     */
    static JacksonJsonProvider createProvider() {
        CustomJacksonJsonProvider provider = new CustomJacksonJsonProvider();
        provider.addDeserialiser(QueueNextResponse.class,
                new NMPIQueueResponseDeserialiser());
        return provider;
    }
}

/**
 * How to understand messages coming from the queue.
 */
@SuppressWarnings("serial")
class NMPIQueueResponseDeserialiser
        extends PropertyBasedDeserialiser<QueueNextResponse> {
    /**
     * Make a deserialiser.
     */
	NMPIQueueResponseDeserialiser() {
        super(QueueNextResponse.class);
        register("id", Job.class);
        register("warning", QueueEmpty.class);
    }
}
