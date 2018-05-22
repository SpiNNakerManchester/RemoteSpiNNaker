package uk.ac.manchester.cs.spinnaker.rest;

import static javax.ws.rs.core.MediaType.MEDIA_TYPE_WILDCARD;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import uk.ac.manchester.cs.spinnaker.job.nmpi.DataItem;

/**
 * The REST API for the output manager.
 */
@Path("/output")
public interface OutputManager {
	/**
	 * Adds outputs to be hosted for a given id, returning a matching list of
	 * URLs on which the files are hosted.
	 *
	 * @param projectId
	 *            The id of the project
	 * @param id
	 *            The id of the job
	 * @param rootFile
	 *            The root directory containing all the files
	 * @param outputs
	 *            The files to add
	 * @return A list of DataItem instances for adding to the job
	 * @throws IOException
	 *             If anything goes wrong.
	 */
	List<DataItem> addOutputs(String projectId, int id, File rootFile,
			Collection<File> outputs) throws IOException;

	/**
	 * Gets a results file.
	 *
	 * @param projectId
	 *            The id of the project which owned the job.
	 * @param id
	 *            The id of the job which produced the file.
	 * @param filename
	 *            The name of the file.
	 * @param download
	 *            Whether to obtain the file as a "download".
	 * @return A response containing the file, or a "NOT FOUND" response if the
	 *         file does not exist.
	 */
	// TODO: Enable authentication based on collab id
	// @PreAuthorize("@collabSecurityService.canRead(#projectId)")
	@GET
	@Path("{projectId}/{id}/{filename:.*}")
	@Produces(MEDIA_TYPE_WILDCARD)
	Response getResultFile(@PathParam("projectId") String projectId,
			@PathParam("id") int id, @PathParam("filename") String filename,
			@QueryParam("download") @DefaultValue("true") boolean download);

	/**
	 * Retrieve a result file.
	 *
	 * @param id
	 *            The job ID.
	 * @param filename
	 *            The filename relative to the job working directory.
	 * @param download
	 *            Whether to obtain the file as a "download".
	 * @return A response containing the file, or a "NOT FOUND" response if the
	 *         file does not exist.
	 */
	@GET
	@Path("{id}/{filename:.*}")
	@Produces(MEDIA_TYPE_WILDCARD)
	Response getResultFile(@PathParam("id") int id,
			@PathParam("filename") String filename,
			@QueryParam("download") @DefaultValue("true") boolean download);

	/**
	 * Upload a file to the HPC store.
	 *
	 * @param projectId
	 *            The project ID
	 * @param id
	 *            The job ID
	 * @param serverUrl
	 *            The HPC storage service
	 * @param storageId
	 *            The ID for the storage on the HPC service
	 * @param filePath
	 *            The path within the storage
	 * @param userId
	 *            The HPC user ID
	 * @param token
	 *            The auth token
	 * @return Description of whether the upload was successful.
	 */
	@POST
	@Produces(TEXT_PLAIN)
	@Path("{projectId}/{id}/uploadToHPC")
	// TODO: Enable authentication based on collab id
	// @PreAuthorize("@collabSecurityService.canWrite(#projectId)")
	Response uploadResultsToHPCServer(@PathParam("projectId") String projectId,
			@PathParam("id") int id, @QueryParam("url") String serverUrl,
			@QueryParam("storageId") String storageId,
			@QueryParam("filePath") String filePath,
			@QueryParam("userId") String userId,
			@QueryParam("token") String token);
	// What is body of the POST? What is the type of the response?
}
