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
package uk.ac.manchester.cs.spinnaker.nmpi;

import static org.joda.time.DateTimeZone.UTC;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.createApiKeyClient;
import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.createBasicClient;
import static uk.ac.manchester.cs.spinnaker.utils.ThreadUtils.sleep;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.manchester.cs.spinnaker.job.nmpi.DataItem;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job.nmpi.QueueEmpty;
import uk.ac.manchester.cs.spinnaker.job.nmpi.QueueNextResponse;
import uk.ac.manchester.cs.spinnaker.model.NMPILog;
import uk.ac.manchester.cs.spinnaker.rest.NMPIQueue;

/**
 * Manages the NMPI queue, receiving jobs and submitting them to be run.
 */
public class NMPIQueueManager {

    /**
     * Job status when finished.
     */
    public static final String STATUS_FINISHED = "finished";

    /**
     * Job status when in the queue but the executer hasn't started.
     */
    public static final String STATUS_QUEUED = "queued";

    /**
     * Job status when running.
     */
    public static final String STATUS_RUNNING = "running";

    /**
     * Job status when in error.
     */
    public static final String STATUS_ERROR = "error";

    /**
     * The amount of time to sleep when an empty queue is detected.
     */
    private static final int EMPTY_QUEUE_SLEEP_MS = 10000;

    /** The queue to get jobs from. */
    private NMPIQueue queue;
    /** Marker to indicate if the manager is done or not. */
    private boolean done = false;
    /** The set of listeners for this queue. */
    private final Set<NMPIQueueListener> listeners = new HashSet<>();
    /** A cache of jobs that have been received. */
    private final Map<Integer, Job> jobCache = new HashMap<>();
    /** The log of the job so far. */
    private final Map<Integer, NMPILog> jobLog = new HashMap<>();
    /**
     * Logger.
     */
    private static final Logger logger = getLogger(NMPIQueueManager.class);

    /** The hardware identifier for the queue. */
    @Value("${nmpi.hardware}")
    private String hardware;
    /** The URL from which to load the data. */
    @Value("${nmpi.url}")
    private URL nmpiUrl;
    /** The username to log in to the server with. */
    @Value("${nmpi.username}")
    private String nmpiUsername;
    /** The password or API key to log in to the server with. */
    @Value("${nmpi.password}")
    private String nmpiPassword;
    /**
     * True if the password is an API key, False if the password should be used
     * to obtain the key.
     */
    @Value("${nmpi.passwordIsApiKey}")
    private boolean nmpiPasswordIsApiKey;

    /**
     * Initialise the client.
     */
    @PostConstruct
    private void initAPIClient() {
        var apiKey = nmpiPassword;
        if (!nmpiPasswordIsApiKey) {
            queue = createBasicClient(nmpiUrl, nmpiUsername, nmpiPassword,
                    NMPIQueue.class);
            apiKey = queue.getToken(nmpiUsername).getKey();
        }
        queue = createApiKeyClient(nmpiUrl, nmpiUsername, apiKey,
                NMPIQueue.class, NMPIQueue.createProvider());
    }

    /**
     * Gets a job from the cache, or from the server if the job is not in the
     * cache.
     *
     * @param id
     *            The ID of the job
     * @return The job
     */
    private Job getJob(final int id) {
        synchronized (jobCache) {
            return jobCache.computeIfAbsent(id, queue::getJob);
        }
    }

    /**
     * Register a listener against the manager for new jobs.
     *
     * @param listener
     *            The listener to register
     */
    public void addListener(final NMPIQueueListener listener) {
        listeners.add(listener);
    }

    public void processResponsesFromQueue() {
        while (!done) {
            try {
                // logger.debug("Getting next job");
                processResponse(queue.getNextJob(hardware));
            } catch (final Exception e) {
                logger.error("Error in getting next job", e);
                sleep(EMPTY_QUEUE_SLEEP_MS);
            }
        }
    }

    /**
     * Process the response from the service.
     *
     * @param response The response to process
     */
    private void processResponse(final QueueNextResponse response) {
        if (response instanceof QueueEmpty) {
            sleep(EMPTY_QUEUE_SLEEP_MS);
        } else if (response instanceof Job) {
            processResponse((Job) response);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Process the response of a Job.
     *
     * @param job The job to process
     */
    private void processResponse(final Job job) {
        synchronized (jobCache) {
            jobCache.put(job.getId(), job);
        }
        logger.debug("Job {} received", job.getId());
        try {
            for (final var listener : listeners) {
                listener.addJob(job);
            }
            logger.debug("Setting job status to queued");
            job.setTimestampSubmission(
                    job.getTimestampSubmission().withZoneRetainFields(UTC));
            job.setTimestampCompletion(null);
            job.setStatus(STATUS_QUEUED);
            logger.debug("Updating job status on server");
            queue.updateJob(job.getId(), job);
        } catch (final IOException e) {
            logger.error("Error in executing job", e);
            setJobError(job.getId(), null, null, e, 0, null);
        }
    }

    /**
     * Appends log messages to the log.
     *
     * @param id
     *            The ID of the job
     * @param logToAppend
     *            The messages to append
     */
    public void appendJobLog(final int id, final String logToAppend) {
        var existingLog = jobLog.computeIfAbsent(id, ignored -> new NMPILog());
        existingLog.appendContent(logToAppend);
        logger.debug("Job {} log is being updated", id);
        queue.updateLog(id, existingLog);
    }

    /**
     * Mark a job as running.
     *
     * @param id
     *            The ID of the job.
     */
    public void setJobRunning(final int id) {
        logger.debug("Job {} is running", id);
        final var job = getJob(id);
        job.setStatus(STATUS_RUNNING);
        logger.debug("Updating job status on server");
        queue.updateJob(id, job);
    }

    /**
     * Marks a job as finished successfully.
     *
     * @param id
     *            The ID of the job
     * @param logToAppend
     *            Any additional log messages to append to the existing log
     *            (null if none)
     * @param outputs
     *            The outputs of the job (null if none)
     * @param resourceUsage
     *            The amount of resources used
     * @param provenance
     *            JSON provenance information
     */
    public void setJobFinished(final int id, final String logToAppend,
            final List<DataItem> outputs, final long resourceUsage,
            final ObjectNode provenance) {
        logger.debug("Job {} is finished", id);

        if (logToAppend != null) {
            appendJobLog(id, logToAppend);
        }

        final var job = getJob(id);
        job.setStatus(STATUS_FINISHED);
        job.setOutputData(outputs);
        job.setTimestampCompletion(new DateTime(UTC));
        job.setResourceUsage(resourceUsage);
        job.setProvenance(provenance);

        logger.debug("Updating job status on server");
        queue.updateJob(id, job);

        jobLog.remove(id);
        jobCache.remove(id);
    }

    /**
     * Marks a job as finished with an error.
     *
     * @param id
     *            The ID of the job
     * @param logToAppend
     *            Any additional log messages to append to the existing log
     *            (null if none)
     * @param outputs
     *            Any outputs generated, or null if none
     * @param error
     *            The error details
     * @param resourceUsage
     *            The amount of resources used
     * @param provenance
     *            JSON provenance information
     */
    public void setJobError(final int id, final String logToAppend,
            final List<DataItem> outputs, final Throwable error,
            final long resourceUsage, final ObjectNode provenance) {
        logger.debug("Job {} finished with an error", id);
        final var errors = new StringWriter();
        error.printStackTrace(new PrintWriter(errors));
        final var logMessage = new StringBuilder();
        if (logToAppend != null) {
            logMessage.append(logToAppend);
        }
        if (jobLog.containsKey(id) || (logMessage.length() > 0)) {
            logMessage.append("\n\n==================\n");
        }
        logMessage.append("Error:\n");
        logMessage.append(errors.toString());
        appendJobLog(id, logMessage.toString());

        final var job = getJob(id);
        job.setStatus(STATUS_ERROR);
        job.setTimestampCompletion(new DateTime(UTC));
        job.setOutputData(outputs);
        job.setResourceUsage(resourceUsage);
        job.setProvenance(provenance);

        logger.debug("Updating job on server");
        queue.updateJob(id, job);

        jobLog.remove(id);
        jobCache.remove(id);
    }

    /**
     * Close the manager.
     */
    public void close() {
        done = true;
    }
}
