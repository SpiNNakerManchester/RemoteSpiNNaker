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
package uk.ac.manchester.cs.spinnaker.output;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.move;
import static java.nio.file.Files.probeContentType;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.createBearerClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.cs.spinnaker.job.nmpi.DataItem;
import uk.ac.manchester.cs.spinnaker.rest.OutputManager;
import uk.ac.manchester.cs.spinnaker.rest.UnicoreFileClient;

/**
 * Service for managing Job output files.
 */
//TODO needs security; Role = OutputHandler
public class OutputManagerImpl implements OutputManager {

    /**
     * Indicates that a file has been removed.
     */
    private static final String PURGED_FILE = ".purged_";

    /**
     * The directory to store files in.
     */
    @Value("${results.directory}")
    private File resultsDirectory;

    /**
     * The URL of the server.
     */
    private final URL baseServerUrl;

    /**
     * The amount of time results should be kept, in milliseconds.
     */
    private long timeToKeepResults;

    /**
     * Map of locks for files.
     */
    private final Map<File, LockToken> synchronizers = new HashMap<>();

    /**
     * The logger.
     */
    private static final Logger logger = getLogger(OutputManagerImpl.class);

    /**
     * A lock token. Initially locked.
     */
    private static class LockToken {
        /**
         * True if the token is locked.
         */
        private boolean locked = true;

        /**
         * True if the token is waiting for a lock.
         */
        private boolean waiting = false;

        /**
         * Wait until the token is unlocked.
         */
        private synchronized void waitForUnlock() {
            waiting = true;

            // Wait until unlocked
            while (locked) {
                try {
                    wait();
                } catch (final InterruptedException e) {
                    // Do Nothing
                }
            }

            // Now lock again
            locked = true;
            waiting = false;
        }

        /**
         * Unlock the token.
         * @return True if the token is waiting again.
         */
        private synchronized boolean unlock() {
            locked = false;
            notifyAll();
            return waiting;
        }
    }

    /**
     * A class to lock a job.
     */
    private class JobLock implements AutoCloseable {
        /**
         * The directory being locked by this token.
         */
        private File dir;

        /**
         * Create a new lock for a directory.
         * @param dirParam The directory to lock
         */
        JobLock(final File dirParam) {
            this.dir = dirParam;

            LockToken lock;
            synchronized (synchronizers) {
                if (!synchronizers.containsKey(dirParam)) {
                    // Constructed pre-locked
                    synchronizers.put(dirParam, new LockToken());
                    return;
                }
                lock = synchronizers.get(dirParam);
            }

            lock.waitForUnlock();
        }

        @Override
        public void close() {
            synchronized (synchronizers) {
                final LockToken lock = synchronizers.get(dir);
                if (!lock.unlock()) {
                    synchronizers.remove(dir);
                }
            }
        }
    }

    /**
     * Instantiate the output manager.
     *
     * @param baseServerUrlParam
     *            The base URL of the overall service, used when generating
     *            internal URLs.
     */
    public OutputManagerImpl(final URL baseServerUrlParam) {
        this.baseServerUrl = baseServerUrlParam;
    }

    /**
     * Set the number of days after a job has finished to keep results.
     * @param nDaysToKeepResults The number of days to keep the results
     */
    @Value("${results.purge.days}")
    void setPurgeTimeout(final long nDaysToKeepResults) {
        timeToKeepResults = MILLISECONDS.convert(nDaysToKeepResults, DAYS);
    }

    /**
     * Periodic execution engine.
     */
    private final ScheduledExecutorService scheduler = newScheduledThreadPool(
            1);

    /**
     * Arrange for old output to be purged once per day.
     */
    @PostConstruct
    private void initPurgeScheduler() {
        scheduler.scheduleAtFixedRate(this::removeOldFiles, 0, 1, DAYS);
    }

    /**
     * Stop the scheduler running jobs.
     */
    @PreDestroy
    private void stopPurgeScheduler() {
        scheduler.shutdown();
    }

    /**
     * Get the project directory for a given project.
     * @param projectId The id of the project
     * @return The directory of the project
     */
    private File getProjectDirectory(final String projectId) {
        if ((projectId == null) || projectId.isEmpty()
                || projectId.endsWith("/")) {
            throw new IllegalArgumentException("bad projectId");
        }
        final String name = new File(projectId).getName();
        if (name.equals(".") || name.equals("..") || name.isEmpty()) {
            throw new IllegalArgumentException("bad projectId");
        }
        return new File(resultsDirectory, name);
    }

    @Override
    public List<DataItem> addOutputs(final String projectId, final int id,
            final File baseDirectory, final Collection<File> outputs)
            throws IOException {
        if (outputs == null) {
            return null;
        }

        final String pId = new File(projectId).getName();
        final int pathStart = baseDirectory.getAbsolutePath().length();
        final File projectDirectory = getProjectDirectory(projectId);
        final File idDirectory = new File(projectDirectory, String.valueOf(id));

        try (JobLock op = new JobLock(idDirectory)) {
            final List<DataItem> outputData = new ArrayList<>();
            for (final File output : outputs) {
                if (!output.getAbsolutePath()
                        .startsWith(baseDirectory.getAbsolutePath())) {
                    throw new IOException("Output file " + output
                            + " is outside base directory " + baseDirectory);
                }

                String outputPath = output.getAbsolutePath()
                        .substring(pathStart).replace('\\', '/');
                if (outputPath.startsWith("/")) {
                    outputPath = outputPath.substring(1);
                }

                final File newOutput = new File(idDirectory, outputPath);
                newOutput.getParentFile().mkdirs();
                move(output.toPath(), newOutput.toPath());
                final URL outputUrl = new URL(baseServerUrl,
                        "output/" + pId + "/" + id + "/" + outputPath);
                outputData.add(new DataItem(outputUrl.toExternalForm()));
                logger.debug("New output {} mapped to {}",
                        newOutput, outputUrl);
            }

            return outputData;
        }
    }

    /**
     * Get a file as a response to a query.
     * @param idDirectory The directory of the project
     * @param filename The name of the file to be stored
     * @param download
     *     True if the content type should be set to guarantee that the file
     *     is downloaded, False to attempt to guess the content type
     * @return The response
     */
    private Response getResultFile(final File idDirectory,
            final String filename, final boolean download) {
        final File resultFile = new File(idDirectory, filename);
        final File purgeFile = getPurgeFile(idDirectory);

        try (JobLock op = new JobLock(idDirectory)) {
            if (purgeFile.exists()) {
                logger.debug("{} was purged", idDirectory);
                return status(NOT_FOUND).entity("Results from job "
                        + idDirectory.getName() + " have been removed")
                        .build();
            }

            if (!resultFile.canRead()) {
                logger.debug("{} was not found", resultFile);
                return status(NOT_FOUND).build();
            }

            try {
                if (!download) {
                    final String contentType =
                            probeContentType(resultFile.toPath());
                    if (contentType != null) {
                        logger.debug("File has content type {}", contentType);
                        return ok(resultFile, contentType).build();
                    }
                }
            } catch (final IOException e) {
                logger.debug("Content type of {} could not be determined",
                        resultFile, e);
            }

            return ok(resultFile).header("Content-Disposition",
                    "attachment; filename=" + filename).build();
        }
    }

    /**
     * Get the file that marks a directory as purged.
     * @param directory The directory to find the file in
     * @return The purge marker file
     */
    private File getPurgeFile(final File directory) {
        return new File(resultsDirectory, PURGED_FILE + directory.getName());
    }

    @Override
    public Response getResultFile(final String projectId, final int id,
            final String filename, final boolean download) {
        // TODO projectId and id? What's going on?
        logger.debug("Retrieving {} from {}/{}", filename, projectId, id);
        final File projectDirectory = getProjectDirectory(projectId);
        final File idDirectory = new File(projectDirectory, String.valueOf(id));
        return getResultFile(idDirectory, filename, download);
    }

    @Override
    public Response getResultFile(final int id, final String filename,
            final boolean download) {
        // TODO projectId and NO id? What's going on?
        logger.debug("Retrieving {} from {}", filename, id);
        final File idDirectory = getProjectDirectory(String.valueOf(id));
        return getResultFile(idDirectory, filename, download);
    }

    /**
     * Upload files in recursive subdirectories to UniCore.
     * @param directory The directory to start from
     * @param fileManager The UniCore client
     * @param storageId The id of the UniCore storage
     * @param filePath The path in the UniCore storage to upload to
     * @throws IOException If something goes wrong
     */
    private void recursivelyUploadFiles(final File directory,
            final UnicoreFileClient fileManager, final String storageId,
            final String filePath) throws IOException {
        final File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (final File file : files) {
            if (file.getName().equals(".") || file.getName().equals("..")
                    || file.getName().isEmpty()) {
                continue;
            }
            final String uploadFileName = filePath + "/" + file.getName();
            if (file.isDirectory()) {
                recursivelyUploadFiles(file, fileManager, storageId,
                        uploadFileName);
                continue;
            }
            if (!file.isFile()) {
                continue;
            }
            try (FileInputStream input = new FileInputStream(file)) {
                fileManager.upload(storageId, uploadFileName, input);
            } catch (final WebApplicationException e) {
                throw new IOException("Error uploading file to " + storageId
                        + "/" + uploadFileName, e);
            } catch (final FileNotFoundException e) {
                // Ignore files which vanish.
            }
        }
    }

    @Override
    public Response uploadResultsToHPCServer(final String projectId,
            final int id, final String serverUrl, final String storageId,
            final String filePath, final String userId, final String token) {
        // TODO projectId and id? What's going on?
        final File idDirectory =
                new File(getProjectDirectory(projectId), String.valueOf(id));
        if (!idDirectory.canRead()) {
            logger.debug("{} was not found", idDirectory);
            return status(NOT_FOUND).build();
        }

        try {
            final UnicoreFileClient fileClient = createBearerClient(
                    new URL(serverUrl), token, UnicoreFileClient.class);
            try (JobLock op = new JobLock(idDirectory)) {
                recursivelyUploadFiles(idDirectory, fileClient, storageId,
                        filePath.replaceAll("/+$", ""));
            }
        } catch (final MalformedURLException e) {
            logger.error("bad user-supplied URL", e);
            return status(BAD_REQUEST)
                    .entity("The URL specified was malformed").build();
        } catch (final Throwable e) {
            logger.error("failure in upload", e);
            return serverError()
                    .entity("General error reading or uploading a file")
                    .build();
        }

        return ok("ok").build();
    }

    /**
     * Recursively remove a directory.
     * @param directory The directory to remove
     */
    private void removeDirectory(final File directory) {
        for (final File file : directory.listFiles()) {
            if (file.isDirectory()) {
                removeDirectory(file);
            } else {
                file.delete();
            }
        }
        directory.delete();
    }

    /**
     * Remove files that are deemed to have expired.
     */
    private void removeOldFiles() {
        final long startTime = currentTimeMillis();
        for (final File projectDirectory : resultsDirectory.listFiles()) {
            if (projectDirectory.isDirectory()
                    && removeOldProjectDirectoryContents(startTime,
                            projectDirectory)) {
                logger.info("No more outputs for project {}",
                        projectDirectory.getName());
                projectDirectory.delete();
            }
        }
    }

    /**
     * Remove project contents that are deemed to have expired.
     * @param startTime The current time being considered
     * @param projectDirectory The directory containing the project files
     * @return True if every job in the project has been removed
     */
    private boolean removeOldProjectDirectoryContents(final long startTime,
            final File projectDirectory) {
        boolean allJobsRemoved = true;
        for (final File jobDirectory : projectDirectory.listFiles()) {
            logger.debug("Determining whether to remove {} "
                    + "which is {}ms old of {}", jobDirectory,
                    startTime - jobDirectory.lastModified(),
                    timeToKeepResults);
            if (jobDirectory.isDirectory() && ((startTime
                    - jobDirectory.lastModified()) > timeToKeepResults)) {
                logger.info("Removing results for job {}",
                        jobDirectory.getName());
                try (JobLock op = new JobLock(jobDirectory)) {
                    removeDirectory(jobDirectory);
                }

                try (PrintWriter purgedFileWriter =
                        new PrintWriter(getPurgeFile(jobDirectory))) {
                    purgedFileWriter.println(currentTimeMillis());
                } catch (final IOException e) {
                    logger.error("Error writing purge file", e);
                }
            } else {
                allJobsRemoved = false;
            }
        }
        return allJobsRemoved;
    }
}
