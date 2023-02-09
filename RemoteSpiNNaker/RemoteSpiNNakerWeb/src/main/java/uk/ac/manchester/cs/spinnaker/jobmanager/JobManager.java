/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.cs.spinnaker.jobmanager;

import static java.io.File.createTempFile;
import static java.lang.Math.ceil;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.cs.spinnaker.ThreadUtils.waitfor;
import static uk.ac.manchester.cs.spinnaker.nmpi.NMPIQueueManager.STATUS_QUEUED;
import static uk.ac.manchester.cs.spinnaker.nmpi.NMPIQueueManager.STATUS_RUNNING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private static final Logger logger = getLogger(JobManager.class);

    /**
     * Job ID &rarr; Machine(s) allocated.
     */
    private final Map<Integer, List<SpinnakerMachine>> allocatedMachines =
            new HashMap<>();

    /**
     * Executor ID &rarr; Executor.
     */
    private final Map<String, JobExecuter> jobExecuters = new HashMap<>();

    /**
     * Executor ID &rarr; Job ID.
     */
    private final Map<String, Job> executorJobId = new HashMap<>();

    /**
     * Job ID &rarr; Directory of temporary output files.
     */
    private final Map<Integer, File> jobOutputTempFiles = new HashMap<>();

    /**
     * Job ID &rarr; number of cores needed by job.
     */
    private final Map<Integer, Long> jobNCores = new HashMap<>();

    /**
     * Job ID &rarr; Job resource usage (in core-hours).
     */
    private final Map<Integer, Long> jobResourceUsage = new HashMap<>();

    /**
     * Job ID &rarr; Job Provenance data.
     */
    private final Map<Integer, ObjectNode> jobProvenance = new HashMap<>();

    /**
     * Thread group for the executor.
     */
    private ThreadGroup threadGroup;

    /**
     * Calls {@link #updateStatus()} every {@link #STATUS_UPDATE_PERIOD}
     * seconds.
     */
    private final ScheduledExecutorService scheduler =
            newScheduledThreadPool(1);

    /**
     * Create a job manager.
     *
     * @param baseUrlParam The URL of the REST service of the manager.
     */
    public JobManager(final URL baseUrlParam) {
        this.baseUrl = requireNonNull(baseUrlParam);
        logger.info("Base URL is {}", baseUrlParam);
    }

    /**
     * Start the manager's worker threads.
     */
    @PostConstruct
    private void startManager() {
        threadGroup = new ThreadGroup("NMPI");
        // Start the queue manager
        queueManager.addListener(this);
        new Thread(threadGroup, queueManager::processResponsesFromQueue,
                "QueueManager").start();
        scheduler.scheduleAtFixedRate(this::updateStatus,
                0, STATUS_UPDATE_PERIOD, TimeUnit.SECONDS);
    }

    /**
     * Stop the manager's worker threads.
     */
    @PreDestroy
    private void stopManager() {
        scheduler.shutdown();
        queueManager.close(); // Stops the queue manager thread eventually
    }

    @Override
    public void addJob(final Job job) throws IOException {
        requireNonNull(job);
        logger.info("New job {}", job.getId());

        // Add any existing provenance to be updated
        synchronized (jobProvenance) {
            var prov = job.getProvenance();
            if (nonNull(prov)) {
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
        final var executer =
                jobExecuterFactory.createJobExecuter(this, baseUrl);
        synchronized (jobExecuters) {
            var executerId = executer.getExecuterId();
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
            while (isNull(job)) {
                waitfor(jobExecuters);
                job = executorJobId.get(executerId);
            }
        }
        logger.info("Executer {} is running {}", executerId, job.getId());
        queueManager.setJobRunning(job.getId());
        return job;
    }

    @Override
    public SpinnakerMachine getLargestJobMachine(final int id,
            final double runTime) {
        // TODO Check quota to get the largest machine within the quota
        return machineManager.getMachines().stream()
                .max(comparing(SpinnakerMachine::getArea))
                .orElse(null);
    }

    @Override
    public SpinnakerMachine getJobMachine(final int id, final int nCores,
            final int nChips, final int nBoards, final double runTime) {
        // TODO Check quota

        logger.info(
                "Request for {} cores or {} chips or {} boards for {} seconds",
                nCores, nChips, nBoards, runTime / MILLISECONDS_PER_SECOND);

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

        final var machine = allocateMachineForJob(id, nBoardsToRequest);
        logger.info("Running {} on {}", id, machine.getMachineName());
        final long resourceUsage =
                (long) ((runTime / MILLISECONDS_PER_SECOND) * quotaNCores);
        logger.debug("Resource usage: {}", resourceUsage);
        synchronized (jobResourceUsage) {
            jobResourceUsage.put(id, resourceUsage);
            jobNCores.put(id, quotaNCores);
        }
        addProvenance(id, asList("spinnaker_machine"),
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
        var machines = allocatedMachines.get(id);
        if (isNull(machines)) {
            throw new WebApplicationException(
                    "No machines found for job " + id, NOT_FOUND);
        }
        for (var machine : machines) {
            if (machine.getMachineName().equals(machineName)) {
                if (remove) {
                    machines.remove(machine);
                }
                return machine;
            }
        }
        throw new WebApplicationException(
                "Machine " + machineName + " does not exist for job " + id,
                BAD_REQUEST);
    }

    @Override
    public void releaseMachine(final int id, final String machineName) {
        synchronized (allocatedMachines) {
            var machine = findMachine(id, machineName, true);
            machineManager.releaseMachine(machine);
        }
    }

    @Override
    public void setMachinePower(final int id, final String machineName,
            final boolean powerOn) {
        synchronized (allocatedMachines) {
            var machine = findMachine(id, machineName, false);
            machineManager.setMachinePower(machine, powerOn);
        }
    }

    @Override
    public ChipCoordinates getChipCoordinates(final int id,
            final String machineName, final int chipX, final int chipY) {
        synchronized (allocatedMachines) {
            var machine = findMachine(id, machineName, false);
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
        final var machine =
                machineManager.getNextAvailableMachine(nBoardsToRequest);
        synchronized (allocatedMachines) {
            allocatedMachines.computeIfAbsent(
                    id, ignored -> new ArrayList<>()).add(machine);
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
        logger.info("Usage for {} now {}", id, usage);
    }

    @Override
    public JobMachineAllocated checkMachineLease(final int id,
            final int waitTime) {
        final var machines = getMachineForJob(id);

        // Return false if any machine is gone
        if (!machines.stream().allMatch(machineManager::isMachineAvailable)) {
            return new JobMachineAllocated(false);
        }

        // Wait for the state change of any machine
        waitForAnyMachineStateChange(waitTime, machines);

        // Again check for a machine which is gone
        return new JobMachineAllocated(machines.stream()
                .allMatch(machineManager::isMachineAvailable));
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
        final var stateChangeSync = new LinkedBlockingQueue<>();
        for (final var machine : machines) {
            final var stateThread = new Thread(threadGroup, () -> {
                machineManager.waitForMachineStateChange(machine, waitTime);
                stateChangeSync.offer(machine);
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
        logger.debug("Updating log for {}", id);
        logger.trace("{}: {}", id, logToAppend);
        queueManager.appendJobLog(id, requireNonNull(logToAppend));
    }

    @Override
    public void addOutput(final String projectId, final int id,
            final String output, final InputStream input) {
        requireNonNull(output);
        requireNonNull(input);
        try {
            if (!jobOutputTempFiles.containsKey(id)) {
                final var tempOutputDir = createTempFile("jobOutput", ".tmp");
                forceDelete(tempOutputDir);
                forceMkdir(tempOutputDir);
                jobOutputTempFiles.put(id, tempOutputDir);
            }
        } catch (final IOException e) {
            logger.error("Error creating temporary output directory for {}",
                    id, e);
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }

        final var outputFile = new File(jobOutputTempFiles.get(id), output);
        try {
            forceMkdirParent(outputFile);
            copyInputStreamToFile(input, outputFile);
        } catch (final IOException e) {
            logger.error("Error writing file {} for job {}",
                    outputFile, id, e);
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
        final var outputItems = new ArrayList<DataItem>();
        if (nonNull(outputs)) {
            final var outputFiles =
                    outputs.stream().map(File::new).collect(toList());
            outputItems.addAll(outputManager.addOutputs(projectId, id,
                    new File(baseFile), outputFiles));
        }
        final var directory = jobOutputTempFiles.remove(id);
        if (nonNull(directory)) {
            outputItems.addAll(outputManager.addOutputs(projectId, id,
                    directory, listFiles(directory, null, true)));
        }
        return outputItems;
    }

    @Override
    public void addProvenance(final int id, final List<String> path,
            final String value) {
        synchronized (jobProvenance) {
            final var provenance = jobProvenance.computeIfAbsent(id,
                    ignored -> new ObjectNode(JsonNodeFactory.instance));

            // Traverse the object node to find the path to add to
            var current = provenance;
            boolean add = true;
            for (int i = 0; i < path.size() - 1; i++) {
                var item = path.get(i);
                var subNode = current.get(item);

                // If the path is not present, add it
                if (isNull(subNode)) {
                    subNode = current.putObject(item);
                }

                // If the item is an ObjectNode, go to the next item
                if (subNode instanceof ObjectNode) {
                    current = (ObjectNode) subNode;

                // If the item exists and is not an ObjectNode, this is an
                // error as a non-object can't contain values
                } else {
                    add = false;
                    logger.warn("Could not add provenance item {} to job {}: "
                            + "Node {} is not an object",
                            path, id, item);
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
            final var ru = jobResourceUsage.remove(id);
            if (nonNull(ru)) {
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
        logger.info("Marking job {} as finished", id);
        releaseAllocatedMachines(id);

        // Do these before anything that can throw
        final long resourceUsage = getResourceUsage(id);
        final var prov = getProvenance(id);

        try {
            queueManager.setJobFinished(id, logToAppend,
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
            final var machines = allocatedMachines.remove(id);
            if (nonNull(machines)) {
                machines.forEach(machineManager::releaseMachine);
            }
            return nonNull(machines);
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

        logger.info("Marking job {} as error", id);
        releaseAllocatedMachines(id);

        final var exception = reconstructRemoteException(error, stackTrace);
        // Do these before anything that can throw
        final long resourceUsage = getResourceUsage(id);
        final var prov = getProvenance(id);

        try {
            queueManager.setJobError(id, logToAppend,
                    getOutputFiles(projectId, id, baseDirectory, outputs),
                    exception, resourceUsage, prov);
        } catch (final IOException e) {
            logger.error("Error creating URLs while updating job", e);
        }
    }

    /**
     * Convert a remote exception to a local one.
     *
     * @param error The error message.
     * @param stackTrace The stack trace.
     * @return The exception.
     */
    private Exception reconstructRemoteException(final String error,
            final RemoteStackTrace stackTrace) {
        final var exception = new Exception(error);
        exception.setStackTrace(stackTrace.getElements().stream()
                .map(RemoteStackTraceElement::toSTE)
                .toArray(StackTraceElement[]::new));
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
        if (nonNull(job)) {
            final int id = job.getId();

            switch (job.getStatus()) {
            case STATUS_QUEUED:
            case STATUS_RUNNING:
                logger.debug("Executer {} for Job {} has exited, "
                        + "but job not exited cleanly", executorId, id);
                releaseAllocatedMachines(id);
                final long resourceUsage = getResourceUsage(id);
                final var prov = getProvenance(id);
                try {
                    final var projectId =
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
                break;
            default:
                logger.debug("Executer {} for Job {} has exited",
                        executorId, id);
            }
        } else {
            logger.error("An executer {} has exited without a job. "
                    + "This could indicate an error!", executorId);
            logger.error(logToAppend);

            if (restartJobExecuterOnFailure) {
                logger.warn("Restarting of executers is currently disabled");
            }
        }
    }

    @Override
    public Response getJobProcessManager() {
        final var jobManagerStream =
                getClass().getResourceAsStream("/" + JOB_PROCESS_MANAGER_ZIP);
        if (isNull(jobManagerStream)) {
            throw new UnsatisfiedLinkError(
                    JOB_PROCESS_MANAGER_ZIP + " not found in classpath");
        }
        return ok(jobManagerStream).type(APPLICATION_ZIP).build();
    }

    @Override
    public Response getSetupScript() throws IOException {
        return ok(setupScript.getInputStream())
                .type(APPLICATION_OCTET_STREAM).build();
    }

    /**
     * Updates the status.
     */
    private void updateStatus() {
        int nBoardsInUse = 0;
        synchronized (allocatedMachines) {
            for (var machines: allocatedMachines.values()) {
                nBoardsInUse += machines.stream()
                        .mapToInt(SpinnakerMachine::getnBoards).sum();
            }
        }
        statusMonitorManager.updateStatus(
                jobExecuters.size(), nBoardsInUse);
    }
}
