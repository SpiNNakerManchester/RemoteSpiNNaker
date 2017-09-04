package uk.ac.manchester.cs.spinnaker.output;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.move;
import static java.nio.file.Files.probeContentType;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.cs.spinnaker.job.nmpi.DataItem;
import uk.ac.manchester.cs.spinnaker.rest.OutputManager;
import uk.ac.manchester.cs.spinnaker.rest.UnicoreFileClient;

//TODO needs security; Role = OutputHandler
public class OutputManagerImpl implements OutputManager {
    private static final String PURGED_FILE = ".purged_";

    @Value("${results.directory}")
    private File resultsDirectory;
    private final URL baseServerUrl;
    private long timeToKeepResults;
    private final Map<File, JobLock.Token> synchronizers = new HashMap<>();
    private final Logger logger = getLogger(getClass());

    private class JobLock implements AutoCloseable {
        private class Token {
            private boolean locked = true;
            private boolean waiting = false;

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

            private synchronized boolean unlock() {
                locked = false;
                notifyAll();
                return waiting;
            }
        }

        private File dir;
        JobLock(final File dir) {
            this.dir = dir;

            Token lock;
            synchronized (synchronizers) {
                if (!synchronizers.containsKey(dir)) {
                    // Constructed pre-locked
                    synchronizers.put(dir, new Token());
                    return;
                }
                lock = synchronizers.get(dir);
            }

            lock.waitForUnlock();
        }

        @Override
        public void close() {
            synchronized (synchronizers) {
                final Token lock = synchronizers.get(dir);
                if (!lock.unlock()) {
                    synchronizers.remove(dir);
                }
            }
        }
    }

    public OutputManagerImpl(final URL baseServerUrl) {
        this.baseServerUrl = baseServerUrl;
    }

    @Value("${results.purge.days}")
    void setPurgeTimeout(final long nDaysToKeepResults) {
        timeToKeepResults = MILLISECONDS.convert(nDaysToKeepResults, DAYS);
    }

    @PostConstruct
    void initPurgeScheduler() {
        final ScheduledExecutorService scheduler = newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                removeOldFiles();
            }
        }, 0, 1, DAYS);
    }

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
                logger.debug(
                        "New output " + newOutput + " mapped to " + outputUrl);
            }

            return outputData;
        }
    }

    private Response getResultFile(final File idDirectory,
            final String filename, final boolean download) {
        final File resultFile = new File(idDirectory, filename);
        final File purgeFile = getPurgeFile(idDirectory);

        try (JobLock op = new JobLock(idDirectory)) {
            if (purgeFile.exists()) {
                logger.debug(idDirectory + " was purged");
                return Response
                        .status(NOT_FOUND).entity("Results from job "
                                + idDirectory.getName() + " have been removed")
                        .build();
            }

            if (!resultFile.canRead()) {
                logger.debug(resultFile + " was not found");
                return Response.status(NOT_FOUND).build();
            }

            try {
                if (!download) {
                    final String contentType = probeContentType(
                            resultFile.toPath());
                    if (contentType != null) {
                        logger.debug("File has content type " + contentType);
                        return Response.ok(resultFile, contentType).build();
                    }
                }
            } catch (final IOException e) {
                logger.debug("Content type of " + resultFile
                        + " could not be determined", e);
            }

            return Response.ok(resultFile).header("Content-Disposition",
                    "attachment; filename=" + filename).build();
        }
    }

    private File getPurgeFile(final File directory) {
        return new File(resultsDirectory, PURGED_FILE + directory.getName());
    }

    @Override
    public Response getResultFile(final String projectId, final int id,
            final String filename, final boolean download) {
        // TODO projectId and id? What's going on?
        logger.debug(
                "Retrieving " + filename + " from " + projectId + "/" + id);
        final File projectDirectory = getProjectDirectory(projectId);
        final File idDirectory = new File(projectDirectory, String.valueOf(id));
        return getResultFile(idDirectory, filename, download);
    }

    @Override
    public Response getResultFile(final int id, final String filename,
            final boolean download) {
        // TODO projectId and NO id? What's going on?
        logger.debug("Retrieving " + filename + " from " + id);
        final File idDirectory = getProjectDirectory(String.valueOf(id));
        return getResultFile(idDirectory, filename, download);
    }

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
        final File idDirectory = new File(getProjectDirectory(projectId),
                String.valueOf(id));
        if (!idDirectory.canRead()) {
            logger.debug(idDirectory + " was not found");
            return Response.status(NOT_FOUND).build();
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
            return Response.status(BAD_REQUEST)
                    .entity("The URL specified was malformed").build();
        } catch (final Throwable e) {
            logger.error("failure in upload", e);
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity("General error reading or uploading a file")
                    .build();
        }

        return Response.ok().entity("ok").build();
    }

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

    private void removeOldFiles() {
        final long startTime = currentTimeMillis();
        for (final File projectDirectory : resultsDirectory.listFiles()) {
            if (projectDirectory.isDirectory()
                    && removeOldProjectDirectoryContents(startTime,
                            projectDirectory)) {
                logger.info("No more outputs for project "
                        + projectDirectory.getName());
                projectDirectory.delete();
            }
        }
    }

    private boolean removeOldProjectDirectoryContents(final long startTime,
            final File projectDirectory) {
        boolean allJobsRemoved = true;
        for (final File jobDirectory : projectDirectory.listFiles()) {
            logger.debug("Determining whether to remove " + jobDirectory
                    + " which is " + (startTime - jobDirectory.lastModified())
                    + "ms old of " + timeToKeepResults);
            if (jobDirectory.isDirectory() && ((startTime
                    - jobDirectory.lastModified()) > timeToKeepResults)) {
                logger.info(
                        "Removing results for job " + jobDirectory.getName());
                try (JobLock op = new JobLock(jobDirectory)) {
                    removeDirectory(jobDirectory);
                }

                try (PrintWriter purgedFileWriter = new PrintWriter(
                        getPurgeFile(jobDirectory))) {
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
