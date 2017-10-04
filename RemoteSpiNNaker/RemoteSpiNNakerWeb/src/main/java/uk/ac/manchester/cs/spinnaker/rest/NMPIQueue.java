package uk.ac.manchester.cs.spinnaker.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job.nmpi.QueueNextResponse;
import uk.ac.manchester.cs.spinnaker.model.APIKeyResponse;
import uk.ac.manchester.cs.spinnaker.model.NMPILog;

/**
 * The REST API for the NMPI queue.
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
}
