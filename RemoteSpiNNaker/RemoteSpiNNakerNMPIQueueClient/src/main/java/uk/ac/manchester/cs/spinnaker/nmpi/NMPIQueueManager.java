package uk.ac.manchester.cs.spinnaker.nmpi;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import uk.ac.manchester.cs.spinnaker.nmpi.model.Job;
import uk.ac.manchester.cs.spinnaker.nmpi.model.QueueEmpty;
import uk.ac.manchester.cs.spinnaker.nmpi.model.QueueNextResponse;
import uk.ac.manchester.cs.spinnaker.nmpi.rest.NMPIJacksonJsonProvider;
import uk.ac.manchester.cs.spinnaker.nmpi.rest.NMPIQueue;
import uk.ac.manchester.cs.spinnaker.nmpi.rest.QueueResponseDeserialiser;

/**
 * Manages the NMPI queue, receiving jobs and submitting them to be run
 */
@Path("output")
public class NMPIQueueManager extends Thread {

	/**
	 * The amount of time to sleep when an empty queue is detected
	 */
	private static final int EMPTY_QUEUE_SLEEP_MS = 10000;

	/**
	 * The queue to get jobs from
	 */
	private NMPIQueue queue = null;

	/**
	 * Marker to indicate if the manager is done or not
	 */
	private boolean done = false;

	/**
	 * The hardware identifier for the queue
	 */
	private String hardware = null;

	/**
	 * The place where results should be stored
	 */
	private File resultsDirectory = null;

	/**
	 * The set of listeners for this queue
	 */
	private Set<NMPIQueueListener> listeners = new HashSet<NMPIQueueListener>();

	/**
	 * A cache of jobs that have been received
	 */
	private Map<Integer, Job> jobCache = new HashMap<Integer, Job>();

	/**
	 * The URL of the server up to this application
	 */
	private URL baseServerUrl = null;

	/**
	 * The logger
	 */
	private Log logger = LogFactory.getLog(getClass());

	/**
	 * Creates a new Manager, pointing at a queue at a specific URL
	 * @param url The URL from which to load the data
	 * @param hardware The name of the hardware that this queue is for
	 * @param resultsDirectory The directory where results are stored
	 * @param baseServerUrl The URL of the server up to this rest resource
	 * @param username The username to log in to the server with
	 * @param password The password to log in to the server with
	 */
	public NMPIQueueManager(URL url, String hardware,
			File resultsDirectory, URL baseServerUrl, String username,
			String password) {

		NMPIJacksonJsonProvider provider = new NMPIJacksonJsonProvider();
		provider.addDeserialiser(QueueNextResponse.class,
				new QueueResponseDeserialiser());

		// Set up authentication
		HttpHost targetHost = new HttpHost(url.getHost(), url.getPort());
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
		        new AuthScope(targetHost.getHostName(), targetHost.getPort()),
		        new UsernamePasswordCredentials(username, password));
		AuthCache authCache = new BasicAuthCache();
		AuthScheme basicAuth = new BasicScheme();
		authCache.put(new HttpHost("url"), basicAuth);
		BasicHttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		localContext.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);
		DefaultHttpClient httpClient = new DefaultHttpClient();
		ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient,
				localContext);

		ResteasyClient client =
				new ResteasyClientBuilder().httpEngine(engine).build();
		client.register(provider);
        ResteasyWebTarget target = client.target(url.toString());
        queue = target.proxy(NMPIQueue.class);

        this.hardware = hardware;
        this.resultsDirectory = resultsDirectory;
        this.baseServerUrl = baseServerUrl;
	}

	/**
	 * Gets a results file
	 * @param id The id of the job which produced the file
	 * @param filename The name of the file
	 * @return A response containing the file, or a "NOT FOUND" response if the
	 *         file does not exist
	 */
	@GET
	@Path("{id}/{filename}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getResultFile(@PathParam("id") int id,
			@PathParam("filename") String filename) {
		File idDirectory = new File(resultsDirectory, String.valueOf(id));
		File resultFile = new File(idDirectory, filename);

		if (!resultFile.canRead()) {
			return Response.status(Status.NOT_FOUND).build();
		}

		return Response.ok((Object) resultFile)
				.header("Content-Disposition",
						"attachment; filename=" + filename)
				.build();
	}

	/**
	 * Gets a job from the cache, or from the server if the job is not in the
	 * cache
	 * @param id The id of the job
	 * @return The job
	 */
	private Job getJob(int id) {
		synchronized (jobCache) {
			Job job = jobCache.get(id);
			if (job == null) {
				synchronized (queue) {
				    job = queue.getJob(id);
				}
				jobCache.put(id, job);
			}
			return job;
		}
	}

	/**
	 * Register a listener against the manager for new jobs
	 * @param listener The listener to register
	 */
	public void addListener(NMPIQueueListener listener) {
		listeners.add(listener);
	}

	@Override
	public void run() {
	    while (!done) {
	    	try {
	    		logger.debug("Getting next job");
			    QueueNextResponse response = null;
			    synchronized (queue) {
			    	response = queue.getNextJob(hardware);
			    }
			    if (response instanceof QueueEmpty) {
			    	logger.debug("No job received, sleeping");
			    	try {
						Thread.sleep(EMPTY_QUEUE_SLEEP_MS);
					} catch (InterruptedException e) {

						// Do Nothing
					}
			    } else if (response instanceof Job) {
			    	Job job = (Job) response;
			    	synchronized (jobCache) {
			    	    jobCache.put(job.getId(), job);
			    	}
			    	logger.debug("Job " + job.getId() + " received");
			    	try {
			    	    for (NMPIQueueListener listener: listeners) {
							listener.addJob(job.getId(),
									job.getExperimentDescription(),
									job.getInputData(),
									job.getHardwareConfig());
			    	    }
			    	    logger.debug("Setting job status to running");
				    	job.setStatus("running");
				    	logger.debug("Updating job status on server");
				    	synchronized (queue) {
				    	    queue.updateJob(job.getId(), job);
				    	}
					} catch (IOException e) {
						logger.error("Error in executing job", e);
						setJobError(job.getId(), null, e);
					}
			    }
	    	} catch (Exception e) {
	    		logger.error("Error in getting next job", e);
	    		try {
					Thread.sleep(EMPTY_QUEUE_SLEEP_MS);
				} catch (InterruptedException e1) {
					// Do Nothing
				}
	    	}
	    }
	}

	/**
	 * Appends log messages to the log
	 * @param id The id of the job
	 * @param logToAppend The messages to append
	 */
	public void appendJobLog(int id, String logToAppend) {
		Job job = getJob(id);
		String existingLog = job.getLog();
		if (existingLog == null) {
			existingLog = logToAppend;
		} else {
			existingLog += logToAppend;
		}
		job.setLog(existingLog);
		logger.debug("Job " + id + " log is being updated");
		synchronized (queue) {
		    queue.updateJob(id, job);
		}
	}

	/**
	 * Marks a job as finished successfully
	 * @param id The id of the job
	 * @param logToAppend Any additional log messages to append to the existing
	 *        log (null if none)
	 * @param outputs The file outputs of the job (null if none)
	 * @throws MalformedURLException
	 */
	public void setJobFinished(int id, String logToAppend, List<File> outputs)
			throws MalformedURLException {
		logger.debug("Job " + id + " is finished");
		File idDirectory = new File(resultsDirectory, String.valueOf(id));
		idDirectory.mkdirs();
		List<String> outputUrls = new ArrayList<String>();
		if (outputs != null) {
			for (File output : outputs) {
				File newOutput = new File(idDirectory, output.getName());
				output.renameTo(newOutput);
				URL outputUrl = new URL(baseServerUrl,
						id + "/" + output.getName());
				outputUrls.add(outputUrl.toExternalForm());
				logger.debug("New output " + newOutput + " mapped to "
				    + outputUrl);
			}
		}

		Job job = getJob(id);
		job.setStatus("finished");
		job.setOutputData(outputUrls);
		if (logToAppend != null) {
			String existingLog = job.getLog();
			if (existingLog == null) {
				existingLog = logToAppend;
			} else {
				existingLog += logToAppend;
			}
			job.setLog(existingLog);
		}
		job.setTimestampCompletion(new Date());

		logger.debug("Updating job status on server");
		synchronized (queue) {
		    queue.updateJob(id, job);
		}
	}

	/**
	 * Marks a job as finished with an error
	 * @param id The id of the job
	 * @param logToAppend Any additional log messages to append to the existing
	 *        log (null if none)
	 * @param error The error details
	 */
	public void setJobError(int id, String logToAppend, Throwable error) {
		logger.debug("Job " + id + " finished with an error");
		StringWriter errors = new StringWriter();
		error.printStackTrace(new PrintWriter(errors));
		String logMessage = "Error:\n";
		logMessage += errors.toString();

		Job job = getJob(id);
		job.setStatus("error");
		String existingLog = job.getLog();
		if (logToAppend != null) {
			if (existingLog == null) {
				existingLog = logToAppend;
			} else {
				existingLog += logToAppend;
			}
		}
		if (existingLog == null || existingLog.isEmpty()) {
			existingLog = logMessage;
		} else {
			existingLog += "\n\n==================\n";
			existingLog += logMessage;
		}
		job.setLog(existingLog);
		job.setTimestampCompletion(new Date());

		logger.debug("Updating job on server");
		synchronized (queue) {
		    queue.updateJob(id, job);
		}
	}

	/**
	 * Close the manager
	 */
	public void close() {
		done = true;
	}
}
