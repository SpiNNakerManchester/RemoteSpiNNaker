package uk.ac.manchester.cs.spinnaker.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import uk.ac.manchester.cs.spinnaker.model.Icinga2CheckResult;

public interface Icinga2 {

    @Produces("application/json")
    @Consumes("application/json")
    @POST
    @Path("/v1/actions/process-check-result")
    void processCheckResult(Icinga2CheckResult result);
}
