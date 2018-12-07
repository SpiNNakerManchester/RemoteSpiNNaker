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
            @QueryParam("testID") String testID,
            @QueryParam("time") int time);

}
