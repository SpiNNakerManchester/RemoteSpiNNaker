package uk.ac.manchester.cs.spinnaker.job;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

/**
 * Interface to the JobManager.
 */
@Path("/job")
public interface JobManagerInterface {

    /**
     * The Zip media type.
     */
    String APPLICATION_ZIP = "application/zip";

    /**
     * The name of the zip file of the client.
     */
    String JOB_PROCESS_MANAGER_ZIP = "RemoteSpiNNakerJobProcessManager.zip";

    /**
     * Get the next job to execute.
     *
     * @param executerId The ID of the executor.
     * @return The job to be executed.
     */
    @GET
    @Path("next")
    @Produces(APPLICATION_JSON)
    Job getNextJob(@QueryParam("executerId") String executerId);

    /**
     * Get the largest machine available.
     *
     * @param id The ID of the job.
     * @param runTime The run time of the job in seconds.
     * @return The largest spinnaker machine.
     */
    @GET
    @Path("{id}/machine/max")
    @Produces(APPLICATION_JSON)
    SpinnakerMachine getLargestJobMachine(@PathParam("id") int id,
            @QueryParam("runTime") @DefaultValue("-1") double runTime);

    /**
     * Get the machine for a job.
     *
     * @param id The ID of the job
     * @param nCores The number of cores required
     * @param nChips The number of chips required (ignored if nCores != -1)
     * @param nBoards The number of boards required
     *     (ignored if nChips != -1 or nCores != -1)
     * @param runTime The run time of the job in seconds
     * @return The machine allocated to the job.
     */
    @GET
    @Path("{id}/machine")
    @Produces(APPLICATION_JSON)
    SpinnakerMachine getJobMachine(@PathParam("id") int id,
            @QueryParam("nCores") @DefaultValue("-1") int nCores,
            @QueryParam("nChips") @DefaultValue("-1") int nChips,
            @QueryParam("nBoards") @DefaultValue("-1") int nBoards,
            @QueryParam("runTime") @DefaultValue("-1") double runTime);

    /**
     * Check that a machine is still allocated, waiting for it to be unallocated
     * if currently allocated.
     *
     * @param id The id of the machine allocated.
     * @param waitTime The time to wait before a response is provided.
     * @return The allocation status of the machine.
     */
    @GET
    @Path("{id}/machine/checkLease")
    @Produces(APPLICATION_JSON)
    JobMachineAllocated checkMachineLease(@PathParam("id") int id,
            @QueryParam("waitTime") @DefaultValue("10000") int waitTime);

    /**
     * Extend the lease of a machine for a job.
     *
     * @param id The id of the job
     * @param runTime The new total run time in seconds
     */
    @GET
    @Path("{id}/machine/extendLease")
    void extendJobMachineLease(@PathParam("id") int id,
            @QueryParam("runTime") @DefaultValue("-1") double runTime);

    /**
     * Append to the log of a job.
     *
     * @param id The id of the job
     * @param logToAppend The text to append
     */
    @POST
    @Path("{id}/log")
    @Consumes("text/plain")
    void appendLog(@PathParam("id") int id, String logToAppend);

    /**
     * Add provenance data to a job.
     *
     * @param id The ID of the job
     * @param path The path of the data
     * @param value The value to be added
     */
    @POST
    @Path("{id}/provenance")
    void addProvenance(@PathParam("id") int id,
            @QueryParam("name") List<String> path,
            @QueryParam("value") String value);

    /**
     * Add a binary output file to a job.
     *
     * @param projectId The id of the project containing the job.
     * @param id The id of the job
     * @param output The filename of the output file
     * @param input The input stream containing the data
     */
    @POST
    @Path("{projectId}/{id}/addoutput")
    @Consumes(APPLICATION_OCTET_STREAM)
    void addOutput(@PathParam("projectId") String projectId,
            @PathParam("id") int id,
            @QueryParam("outputFilename") String output, InputStream input);

    /**
     * Mark a job as completed.
     *
     * @param projectId The id of the project containing the job.
     * @param id The id of the job.
     * @param logToAppend Any log data to be added
     * @param baseFilename The base filename for any added files
     * @param outputs A list of output URLs
     */
    @POST
    @Path("{projectId}/{id}/finished")
    @Consumes(TEXT_PLAIN)
    void setJobFinished(@PathParam("projectId") String projectId,
            @PathParam("id") int id, String logToAppend,
            @QueryParam("baseFilename") String baseFilename,
            @QueryParam("outputFilename") List<String> outputs);

    /**
     * Mark a job as failed.
     *
     * @param projectId The ID of the project containing the job.
     * @param id The ID of the job.
     * @param error The error message.
     * @param logToAppend Any additional log messages.
     * @param baseFilename The base filename for any added files
     * @param outputs A list of output URLs
     * @param stackTrace A stack trace of the error
     */
    @POST
    @Path("{projectId}/{id}/error")
    @Consumes(APPLICATION_JSON)
    void setJobError(@PathParam("projectId") String projectId,
            @PathParam("id") int id, @QueryParam("error") String error,
            @QueryParam("logToAppend") String logToAppend,
            @QueryParam("baseFilename") String baseFilename,
            @QueryParam("outputFilename") List<String> outputs,
            RemoteStackTrace stackTrace);

    /**
     * Get the JobProcessManager executable zip file.
     *
     * @return The JobProcessManager zip file as data
     */
    @GET
    @Path(JOB_PROCESS_MANAGER_ZIP)
    @Produces(APPLICATION_ZIP)
    Response getJobProcessManager();
}
