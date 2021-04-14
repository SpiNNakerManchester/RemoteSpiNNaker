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
package uk.ac.manchester.cs.spinnaker.jobmanager;

import static java.io.File.createTempFile;
import static java.lang.Math.ceil;
import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.slf4j.LoggerFactory.getLogger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.swing.Timer;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jdk.internal.jline.internal.Log;
import uk.ac.manchester.cs.spinnaker.job.JobMachineAllocated;
import uk.ac.manchester.cs.spinnaker.job.JobManagerInterface;
import uk.ac.manchester.cs.spinnaker.job.RemoteStackTrace;
import uk.ac.manchester.cs.spinnaker.job.RemoteStackTraceElement;
import uk.ac.manchester.cs.spinnaker.job.nmpi.DataItem;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.machine.ChipCoordinates;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;
import uk.ac.manchester.cs.spinnaker.machinemanager.MachineManager;
import uk.ac.manchester.cs.spinnaker.nmpi.NMPIQueueListener;
import uk.ac.manchester.cs.spinnaker.nmpi.NMPIQueueManager;
import uk.ac.manchester.cs.spinnaker.rest.OutputManager;
import uk.ac.manchester.cs.spinnaker.status.StatusMonitorManager;

/**
 * The manager of jobs; synchronises and manages all the ongoing and future
 * processes and machines.
 */
// TODO needs security; Role = JobEngine
public class JobManager implements NMPIQueueListener, JobManagerInterface {

    /**
     * Assumed number of chips on a board.
     */
    private static final double CHIPS_PER_BOARD = 48.0;

    /**
     * Assumed number of cores usable per chip.
     */
    private static final double CORES_PER_CHIP = 15.0;

    /**
     * Default number of boards to request.
     */
    private static final int DEFAULT_N_BOARDS = 3;

    /**
     * Number of milliseconds per second.
     */
    private static final double MILLISECONDS_PER_SECOND = 1000.0;

    /**
     * Threshold before the number of boards is scaled up.
     */
    private static final double SCALE_UP_THRESHOLD = 0.1;

    /**
     * Seconds between status updates.
     */
    public static final int STATUS_UPDATE_PERIOD = 10;

    /**
     * Milliseconds between log updates.
     */
    private static final int LOG_UPDATE_DELAY = 10000;

    /**
     * The name of the JAR containing the job process manager implementation.
     */
    public static final String JOB_PROCESS_MANAGER_JAR =
            "RemoteSpiNNakerJobProcessManager.jar";

    /**
     * The machine manager.
     */
    @Autowired
    private MachineManager machineManager;

    /**
     * The NMPI queue manager.
     */
    @Autowired
    private NMPIQueueManager queueManager;

    /**
     * The output manager.
     */
    @Autowired
    private OutputManager outputManager;

    /**
     * The status updater.
     */
    @Autowired
    private StatusMonitorManager statusMonitorManager;

    /**
     * The base URL of the REST service.
     */
    private final URL baseUrl;

    /**
     * The Job Execution factory.
     */
    @Autowired
    private JobExecuterFactory jobExecuterFactory;

    /**
     * True if jobs should be restarted on failure.
     */
    @Value("${restartJobExecutorOnFailure}")
    private boolean restartJobExecuterOnFailure;

    /**
     * The name of the setup script.
     */
    @Value("${setupScript}")
    private Resource setupScript;

    /**
     * Logging.
     */
    private final Logger logger = getLogger(getClass());

    /**
     * Job ID -> Machine allocated.
     */
    private final Map<Integer, List<SpinnakerMachine>> allocatedMachines =
            new HashMap<>();

    /**
     * Executor ID -> Executor.
     */
    private final Map<String, JobExecuter> jobExecuters = new HashMap<>();

    /**
     * Executor ID -> Job ID.
     */
    private final Map<String, Job> executorJobId = new HashMap<>();

    /**
     * Job ID -> Directory of temporary output files.
     */
    private final Map<Integer, File> jobOutputTempFiles = new HashMap<>();

    /**
     * Job ID -> number of cores needed by job.
     */
    private final Map<Integer, Long> jobNCores = new HashMap<>();

    /**
     * Job ID -> Job resource usage (in core-hours).
     */
    private final Map<Integer, Long> jobResourceUsage = new HashMap<>();

    /**
     * Job ID -> Job Provenance data.
     */
    private final Map<Integer, ObjectNode> jobProvenance = new HashMap<>();

    /**
     * Job ID -> Log messages to be updated.
     *
     * Note this will be copied and replaced during execution.
     */
    private Map<Integer, String> logsToUpdate = new HashMap<>();

    /**
     * The action to take when the log update happens.
     *
     * Use this to synchronise on log updates actually happening.
     */
    private final ActionListener logTimerAction = new ActionListener() {

        @Override
        public void actionPerformed(final ActionEvent event) {
            sendLogs();
        }
    };

    /**
     * The timer to do log sending.
     *
     * Use this to synchronise on the updating of the log strings map.
     */
    private final Timer logUpdateTimer = new Timer(LOG_UPDATE_DELAY,
            logTimerAction);


    /**
     * Thread group for the executor.
     */
    private ThreadGroup threadGroup;

    /**
     * Create a job manager.
     *
     * @param baseUrlParam The URL of the REST service of the manager.
     */
    public JobManager(final URL baseUrlParam) {
        this.baseUrl = requireNonNull(baseUrlParam);
        System.err.println("Base URL is " + baseUrlParam);
    }

    /**
     * Start the manager's worker threads.
     */
    @PostConstruct
    private void startManager() {
        threadGroup = new ThreadGroup("NMPI");
        // Start the queue manager
        queueManager.addListener(this);
        new Thread(threadGroup, queueManager, "QueueManager").start();
        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new StatusUpdater(),
                0, STATUS_UPDATE_PERIOD, TimeUnit.SECONDS);
        logUpdateTimer.start();
    }

    @Override
    public void addJob(final Job job) throws IOException {
        requireNonNull(job);
        logger.info("New job " + job.getId());

        // Add any existing provenance to be updated
        synchronized (jobProvenance) {
            ObjectNode prov = job.getProvenance();
            if (prov != null) {
                jobProvenance.put(job.getId(), prov);
            }
        }

        // Start an executer for the job
        launchExecuter(job);
    }

    /**
     * You need to hold the lock on {@link #jobExecuters} when running this
     * method.
     *
     * @param job The job to execute
     *
     * @throws IOException If there is an error starting the job
     */
    private void launchExecuter(final Job job) throws IOException {
        final JobExecuter executer =
                jobExecuterFactory.createJobExecuter(this, baseUrl);
        synchronized (jobExecuters) {
            String executerId = executer.getExecuterId();
            jobExecuters.put(executerId, executer);
            executorJobId.put(executerId, job);
            jobExecuters.notifyAll();
        }
        executer.startExecuter();
    }

    @Override
    public Job getNextJob(final String executerId) {
        requireNonNull(executerId);
        Job job = null;
        synchronized (jobExecuters) {
            job = executorJobId.get(executerId);
            while (job == null) {
                try {
                    jobExecuters.wait();
                } catch (InterruptedException e) {

                    // Ignore
                }
                job = executorJobId.get(executerId);
            }
        }
        logger.info("Executer " + executerId + " is running " + job.getId());
        queueManager.setJobRunning(job.getId());
        return job;
    }

    @Override
    public SpinnakerMachine getLargestJobMachine(final int id,
            final double runTime) {
        // TODO Check quota to get the largest machine within the quota

        SpinnakerMachine largest = null;
        for (final SpinnakerMachine machine : machineManager.getMachines()) {
            if ((largest == null) || (machine.getArea() > largest.getArea())) {
                largest = machine;
            }
        }

        return largest;
    }

    @Override
    public SpinnakerMachine getJobMachine(final int id, final int nCores,
            final int nChips, final int nBoards, final double runTime) {
        // TODO Check quota

        logger.info("Request for " + nCores + " cores or " + nChips
                + " chips or " + nBoards + " boards for "
                + (runTime / MILLISECONDS_PER_SECOND)
                + " seconds");

        int nBoardsToRequest = nBoards;
        long quotaNCores = (long) (nBoards * CORES_PER_CHIP * CHIPS_PER_BOARD);

        // If nothing specified, use 3 boards
        if ((nBoards <= 0) && (nChips <= 0) && (nCores <= 0)) {
            nBoardsToRequest = DEFAULT_N_BOARDS;
            quotaNCores = (long) (
                DEFAULT_N_BOARDS * CORES_PER_CHIP * CHIPS_PER_BOARD);
        }

        // If boards not specified, use cores or chips
        if (nBoardsToRequest <= 0) {
            double nChipsExact = nChips;
            quotaNCores = (long) (nChipsExact * CORES_PER_CHIP);

            // If chips not specified, use cores
            if (nChipsExact <= 0) {
                nChipsExact = nCores / CORES_PER_CHIP;
                quotaNCores = nCores;
            }

            double nBoardsExact = nChips / CHIPS_PER_BOARD;

            if ((ceil(nBoardsExact) - nBoardsExact) < SCALE_UP_THRESHOLD) {
                nBoardsExact += 1.0;
            }
            if (nBoardsExact < 1.0) {
                nBoardsExact = 1.0;
            }
            nBoardsExact = ceil(nBoardsExact);
            nBoardsToRequest = (int) nBoardsExact;
        }

        final SpinnakerMachine machine =
                allocateMachineForJob(id, nBoardsToRequest);
        logger.info("Running " + id + " on " + machine.getMachineName());
        final long resourceUsage =
                (long) ((runTime / MILLISECONDS_PER_SECOND) * quotaNCores);
        logger.debug("Resource usage " + resourceUsage);
        synchronized (jobResourceUsage) {
            jobResourceUsage.put(id, resourceUsage);
            jobNCores.put(id, quotaNCores);
        }
        addProvenance(id, Arrays.asList(new String[]{"spinnaker_machine"}),
                machine.getMachineName());

        return machine;
    }

    /**
     * Searches the list for the machine with the given name.
     *
     * @param id
     *            The job id.
     * @param machineName
     *            The name of the machine to find.
     * @param remove
     *            Whether the machine found should be removed or not.
     * @return The machine found
     * @throws WebApplicationException if machine not found
     */
    private SpinnakerMachine findMachine(final int id,
            final String machineName, final boolean remove) {
        List<SpinnakerMachine> machines = allocatedMachines.get(id);
        if (machines == null) {
            throw new WebApplicationException(
                "No machines found for job " + id, Status.NOT_FOUND);
        }
        for (int i = 0; i < machines.size(); i++) {
            final SpinnakerMachine machine = machines.get(i);
            if (machine.getMachineName().equals(machineName)) {
                if (remove) {
                    machines.remove(i);
                }
                return machine;
            }
        }
        throw new WebApplicationException(
                "Machine " + machineName + " does not exist for job " + id);
    }

    @Override
    public void releaseMachine(final int id, final String machineName) {
        synchronized (allocatedMachines) {
            SpinnakerMachine machine = findMachine(id, machineName, true);
            machineManager.releaseMachine(machine);
        }
    }

    @Override
    public void setMachinePower(final int id, final String machineName,
            final boolean powerOn) {
        synchronized (allocatedMachines) {
            SpinnakerMachine machine = findMachine(id, machineName, false);
            machineManager.setMachinePower(machine, powerOn);
        }
    }

    @Override
    public ChipCoordinates getChipCoordinates(final int id,
            final String machineName, final int chipX, final int chipY) {
        synchronized (allocatedMachines) {
            SpinnakerMachine machine = findMachine(id, machineName, false);
            return machineManager.getChipCoordinates(machine, chipX, chipY);
        }
    }

    /**
     * Get a machine to run the job on.
     *
     * @param id The ID of the job
     * @param nBoardsToRequest The number of boards to request
     * @return The machine allocated
     */
    private SpinnakerMachine allocateMachineForJob(final int id,
            final int nBoardsToRequest) {
        final SpinnakerMachine machine =
                machineManager.getNextAvailableMachine(nBoardsToRequest);
        synchronized (allocatedMachines) {
            if (!allocatedMachines.containsKey(id)) {
                allocatedMachines.put(id, new ArrayList<SpinnakerMachine>());
            }
            allocatedMachines.get(id).add(machine);
        }
        return machine;
    }

    /**
     * Get the list of machines currently allocated to a job.
     * @param id The id of the job.
     * @return The list of machines for the job.
     */
    private List<SpinnakerMachine> getMachineForJob(final int id) {
        synchronized (allocatedMachines) {
            return allocatedMachines.get(id);
        }
    }

    @Override
    public void extendJobMachineLease(final int id, final double runTime) {
        // TODO Check quota that the lease can be extended

        long usage;
        synchronized (jobResourceUsage) {
            usage = (long) (jobNCores.get(id)
                    * (runTime / MILLISECONDS_PER_SECOND));
            jobResourceUsage.put(id, usage);
        }
        logger.info("Usage for " + id + " now " + usage);
    }

    @Override
    public JobMachineAllocated checkMachineLease(final int id,
            final int waitTime) {
        final List<SpinnakerMachine> machines = getMachineForJob(id);

        // Return false if any machine is gone
        for (final SpinnakerMachine machine : machines) {
            if (!machineManager.isMachineAvailable(machine)) {
                return new JobMachineAllocated(false);
            }
        }

        // Wait for the state change of any machine
        waitForAnyMachineStateChange(waitTime, machines);

        // Again check for a machine which is gone
        for (final SpinnakerMachine machine : machines) {
            if (!machineManager.isMachineAvailable(machine)) {
                return new JobMachineAllocated(false);
            }
        }

        return new JobMachineAllocated(true);
    }

    /**
     * Wait for something to happen to any of a list of machines.
     *
     * @param waitTime
     *            How long to wait
     * @param machines
     *            What to wait for events from.
     */
    private void waitForAnyMachineStateChange(final int waitTime,
            final List<SpinnakerMachine> machines) {
        final BlockingQueue<Object> stateChangeSync =
                new LinkedBlockingQueue<>();
        for (final SpinnakerMachine machine : machines) {
            final Thread stateThread = new Thread(threadGroup, new Runnable() {
                @Override
                public void run() {
                    machineManager.waitForMachineStateChange(machine, waitTime);
                    stateChangeSync.offer(this);
                }
            }, "waiting for " + machine);
            stateThread.setDaemon(true);
            stateThread.start();
        }
        try {
            stateChangeSync.take();
        } catch (final InterruptedException e) {
            // Does Nothing
        }
    }

    @Override
    public void appendLog(final int id, final String logToAppend) {
        synchronized (logUpdateTimer) {
            logger.trace(id + ": " + logToAppend);
            String existing = logsToUpdate.getOrDefault(id, "");
            logsToUpdate.put(id, existing + logToAppend);
        }
    }

    /**
     * Performs the actual sending of logs; activated on a timer.
     */
    private void sendLogs() {
        // Synchronise the running of this action to 1. avoid doing it twice and
        // 2. To allow other things to wait for the update to complete before
        // changing the log any other way.
        synchronized (logTimerAction) {
            // Take a copy of the logs to update to allow processing to continue
            Map<Integer, String> localLogsToUpdate;
            logUpdateTimer.stop();
            synchronized (logUpdateTimer) {
                localLogsToUpdate = logsToUpdate;
                logsToUpdate = new HashMap<>();
                if (localLogsToUpdate.size() > 0) {
                    logger.info("Job log update in progress");
                }
            }

            // Perform the updates using the local copy
            for (int jobId : localLogsToUpdate.keySet()) {
                String message = localLogsToUpdate.get(jobId);
                try {
                    logger.debug("Updating log for " + jobId);
                    queueManager.appendJobLog(jobId, message);
                } catch (Exception e) {
                    logger.debug("Error updating log - will be retried", e);
                    // On failure, re-prepend the message to the logs to be
                    // updated again later.
                    synchronized (logUpdateTimer) {
                        String existing = logsToUpdate.getOrDefault(jobId, "");
                        logsToUpdate.put(jobId, message + existing);
                    }
                }
            }

            // Restart the timer after the attempts have been done to avoid
            // thrashing
            logUpdateTimer.start();
            if (localLogsToUpdate.size() > 0) {
                logger.info("Log update complete");
            }
        }
    }

    @Override
    public void addOutput(final String projectId, final int id,
            final String output, final InputStream input) {
        requireNonNull(output);
        requireNonNull(input);
        try {
            if (!jobOutputTempFiles.containsKey(id)) {
                final File tempOutputDir = createTempFile("jobOutput", ".tmp");
                forceDelete(tempOutputDir);
                forceMkdir(tempOutputDir);
                jobOutputTempFiles.put(id, tempOutputDir);
            }
        } catch (final IOException e) {
            logger.error("Error creating temporary output directory for " + id,
                    e);
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }

        final File outputFile = new File(jobOutputTempFiles.get(id), output);
        try {
            forceMkdirParent(outputFile);
            copyInputStreamToFile(input, outputFile);
        } catch (final IOException e) {
            logger.error("Error writing file " + outputFile + " for job " + id,
                    e);
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the output data items for a job from a list of outputs.
     *
     * @param projectId The ID of the project of the job
     * @param id The ID of the job
     * @param baseFile The base file location for the files
     * @param outputs The output files
     * @return The list of data items.
     * @throws IOException If there was an error dealing with a file.
     */
    private List<DataItem> getOutputFiles(final String projectId, final int id,
            final String baseFile, final List<String> outputs)
            throws IOException {
        final List<DataItem> outputItems = new ArrayList<>();
        if (outputs != null) {
            final List<File> outputFiles = new ArrayList<>();
            for (final String filename : outputs) {
                outputFiles.add(new File(filename));
            }
            outputItems.addAll(outputManager.addOutputs(projectId, id,
                    new File(baseFile), outputFiles));
        }
        final File directory = jobOutputTempFiles.remove(id);
        if (directory != null) {
            outputItems.addAll(outputManager.addOutputs(projectId, id,
                    directory, listFiles(directory, null, true)));
        }
        return outputItems;
    }

    @Override
    public void addProvenance(final int id, final List<String> path,
            final String value) {

        synchronized (jobProvenance) {
            if (!jobProvenance.containsKey(id)) {
                jobProvenance.put(id, new ObjectNode(JsonNodeFactory.instance));
            }
            final ObjectNode provenance = jobProvenance.get(id);

            // Traverse the object node to find the path to add to
            ObjectNode current = provenance;
            boolean add = true;
            for (int i = 0; i < path.size() - 1; i++) {
                String item = path.get(i);
                JsonNode subNode = current.get(item);

                // If the path is not present, add it
                if (subNode == null) {
                    subNode = current.putObject(item);
                }

                // If the item is an ObjectNode, go to the next item
                if (subNode instanceof ObjectNode) {
                    current = (ObjectNode) subNode;

                // If the item exists and is not an ObjectNode, this is an
                // error as a non-object can't contain values
                } else {
                    add = false;
                    logger.warn("Could not add provenance item " + path
                            + " to job " + id + ": Node " + item
                            + " is not an object");
                    break;
                }
            }

            // If we can add the item, add it
            if (add) {
                current.put(path.get(path.size() - 1), value);
            }
        }
    }

    /**
     * Get the provenance for a job.
     *
     * @param id The ID of the job
     * @return The provenance as a JSON data item
     */
    private ObjectNode getProvenance(final int id) {
        synchronized (jobProvenance) {
            return jobProvenance.remove(id);
        }
    }

    /**
     * Get the resources used by a job.
     *
     * @param id The ID of a job
     * @return The resources used by a job
     */
    private long getResourceUsage(final int id) {
        long resourceUsage = 0;
        synchronized (jobResourceUsage) {
            final Long ru = jobResourceUsage.remove(id);
            if (ru != null) {
                resourceUsage = ru;
                jobNCores.remove(id);
            }
        }
        return resourceUsage;
    }

    @Override
    public void setJobFinished(final String projectId, final int id,
            final String logToAppend, final String baseDirectory,
            final List<String> outputs) {
        requireNonNull(projectId);
        requireNonNull(logToAppend);
        requireNonNull(baseDirectory);
        requireNonNull(outputs);
        logger.info("Marking job " + id + " as finished");
        releaseAllocatedMachines(id);

        // Do these before anything that can throw
        final long resourceUsage = getResourceUsage(id);
        final ObjectNode prov = getProvenance(id);
        final String finalLog = makeFinalLog(id, logToAppend);

        try {
            queueManager.setJobFinished(id, finalLog,
                    getOutputFiles(projectId, id, baseDirectory, outputs),
                    resourceUsage, prov);
        } catch (final IOException e) {
            logger.error("Error creating URLs while updating job", e);
        }
    }

    /**
     * Release the machines allocated to a job.
     *
     * @param id The ID of the job
     * @return {@code true} if there were machines removed by this.
     */
    private boolean releaseAllocatedMachines(final int id) {
        synchronized (allocatedMachines) {
            final List<SpinnakerMachine> machines =
                    allocatedMachines.remove(id);
            if (machines != null) {
                for (final SpinnakerMachine machine : machines) {
                    machineManager.releaseMachine(machine);
                }
            }
            return machines != null;
        }
    }

    /**
     * Make a final log message to be appended.
     *
     * @param id The ID of the job to get the log for.
     * @param logToAppend The final log message to be appended.
     * @return All log messages to be appended.
     */
    private String makeFinalLog(final int id, final String logToAppend) {
        // Synchronise this against the action actually happening now, to avoid
        // odd looking logs!
        synchronized (logTimerAction) {
            // Also synchronise against changes actually happening to the log
            // cache
            synchronized (logUpdateTimer) {
                final String finalLog = logsToUpdate.getOrDefault(id, "")
                        + logToAppend;
                logsToUpdate.remove(id);
                return finalLog;
            }
        }
    }

    @Override
    public void setJobError(final String projectId, final int id,
            final String error, final String logToAppend,
            final String baseDirectory, final List<String> outputs,
            final RemoteStackTrace stackTrace) {
        requireNonNull(projectId);
        requireNonNull(error);
        requireNonNull(logToAppend);
        requireNonNull(baseDirectory);
        requireNonNull(outputs);
        requireNonNull(stackTrace);

        logger.info("Marking job " + id + " as error");
        releaseAllocatedMachines(id);

        // Do these before anything that can throw
        final long resourceUsage = getResourceUsage(id);
        final ObjectNode prov = getProvenance(id);
        final Exception exception =
                reconstructRemoteException(error, stackTrace);
        final String finalLog = makeFinalLog(id, logToAppend);

        try {
            queueManager.setJobError(id, finalLog,
                    getOutputFiles(projectId, id, baseDirectory, outputs),
                    exception, resourceUsage, prov);
        } catch (final IOException e) {
            logger.error("Error creating URLs while updating job", e);
        }
    }

    /**
     * An empty stack trace element.
     */
    private static final StackTraceElement[] STE_TMPL =
            new StackTraceElement[0];

    /**
     * Convert a remote exception to a local one.
     *
     * @param error The error message.
     * @param stackTrace The stack trace.
     * @return The exception.
     */
    private Exception reconstructRemoteException(final String error,
            final RemoteStackTrace stackTrace) {
        final ArrayList<StackTraceElement> elements = new ArrayList<>();
        for (final RemoteStackTraceElement element : stackTrace.getElements()) {
            elements.add(element.toSTE());
        }

        final Exception exception = new Exception(error);
        exception.setStackTrace(elements.toArray(STE_TMPL));
        return exception;
    }

    /**
     * Mark the executor as having exited.
     *
     * @param executorId
     *            The ID of the executor in question
     * @param logToAppend
     *            The log messages
     */
    public void setExecutorExited(final String executorId,
            final String logToAppend) {
        Job job = null;
        synchronized (jobExecuters) {
            job = executorJobId.remove(requireNonNull(executorId));
            jobExecuters.remove(executorId);
        }
        if (job != null) {
            final int id = job.getId();
            logger.debug(
                "Executer " + executorId + " for Job " + id + " has exited");

            String status = job.getStatus();
            if (status == NMPIQueueManager.STATUS_QUEUED
                    || status == NMPIQueueManager.STATUS_RUNNING) {
                logger.debug("Job " + id + " has not exited cleanly");
                releaseAllocatedMachines(id);
                final long resourceUsage = getResourceUsage(id);
                final ObjectNode prov = getProvenance(id);
                try {
                    final String projectId =
                        new File(job.getCollabId()).getName();
                    queueManager.setJobError(id, logToAppend,
                            getOutputFiles(projectId, id, null, null),
                            new Exception("Job did not finish cleanly"),
                            resourceUsage, prov);
                } catch (final IOException e) {
                    logger.error("Error creating URLs while updating job", e);
                    queueManager.setJobError(id, logToAppend,
                            new ArrayList<DataItem>(),
                            new Exception("Job did not finish cleanly"),
                            resourceUsage, prov);
                }
            }
        } else {
            logger.error(
                    "An executer " + executorId + " has exited without a job. "
                            + "This could indicate an error!");
            logger.error(logToAppend);

            if (restartJobExecuterOnFailure) {
                logger.warn("Restarting of executers is currently disabled");
            }
        }
    }

    @Override
    public Response getJobProcessManager() {
        final InputStream jobManagerStream =
                getClass().getResourceAsStream("/" + JOB_PROCESS_MANAGER_ZIP);
        if (jobManagerStream == null) {
            throw new UnsatisfiedLinkError(
                    JOB_PROCESS_MANAGER_ZIP + " not found in classpath");
        }
        return Response.ok(jobManagerStream).type(APPLICATION_ZIP).build();
    }

    @Override
    public Response getSetupScript() throws IOException {
        final InputStream setupScriptStream =
                setupScript.getInputStream();
        return Response.ok(setupScriptStream).type(
                APPLICATION_OCTET_STREAM).build();
    }

    /**
     * Updates the status.
     */
    private class StatusUpdater implements Runnable {

        @Override
        public void run() {
            int nBoardsInUse = 0;
            synchronized (allocatedMachines) {
                for (List<SpinnakerMachine> machines
                        : allocatedMachines.values()) {
                    for (SpinnakerMachine machine : machines) {
                        nBoardsInUse += machine.getnBoards();
                    }
                }
            }
            statusMonitorManager.updateStatus(
                    jobExecuters.size(), nBoardsInUse);
        }
    }
}
