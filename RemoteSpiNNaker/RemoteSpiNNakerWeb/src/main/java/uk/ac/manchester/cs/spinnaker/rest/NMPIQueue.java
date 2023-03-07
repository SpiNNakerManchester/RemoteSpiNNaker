/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        var provider = new CustomJacksonJsonProvider();
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
