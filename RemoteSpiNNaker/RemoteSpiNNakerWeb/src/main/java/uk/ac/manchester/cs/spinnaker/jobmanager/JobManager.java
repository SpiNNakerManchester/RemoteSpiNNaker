package uk.ac.manchester.cs.spinnaker.jobmanager;

import static java.io.File.createTempFile;
import static java.lang.Math.ceil;
import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.slf4j.LoggerFactory.getLogger;

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
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * The manager of jobs; synchronises and manages all the ongoing and future
 * processes and machines.
 */
// TODO needs security; Role = JobEngine
public class JobManager implements NMPIQueueListener, JobManagerInterface {
    private static final double CHIPS_PER_BOARD = 48.0;
    private static final double CORES_PER_CHIP = 15.0;
    public static final String JOB_PROCESS_MANAGER_JAR =
            "RemoteSpiNNakerJobProcessManager.jar";

    @Autowired
    private MachineManager machineManager;
    @Autowired
    private NMPIQueueManager queueManager;
    @Autowired
    private OutputManager outputManager;
    private final URL baseUrl;
    @Autowired
    private JobExecuterFactory jobExecuterFactory;
    @Value("${restartJobExecutorOnFailure}")
    private boolean restartJobExecuterOnFailure;

    private final Logger logger = getLogger(getClass());
    private final Map<Integer, List<SpinnakerMachine>> allocatedMachines =
            new HashMap<>();
    private final BlockingQueue<Job> jobsToRun = new LinkedBlockingQueue<>();
    private final Map<String, JobExecuter> jobExecuters = new HashMap<>();
    private final Map<String, Job> executorJobId = new HashMap<>();
    private final Map<Integer, File> jobOutputTempFiles = new HashMap<>();
    private final Map<Integer, Long> jobNCores = new HashMap<>();
    private final Map<Integer, Long> jobResourceUsage = new HashMap<>();
    private final Map<Integer, ObjectNode> jobProvenance =
            new HashMap<>();
    private ThreadGroup threadGroup;

    public JobManager(final URL baseUrl) {
        this.baseUrl = requireNonNull(baseUrl);
    }

    @PostConstruct
    void startManager() {
        threadGroup = new ThreadGroup("NMPI");
        // Start the queue manager
        queueManager.addListener(this);
        new Thread(threadGroup, queueManager, "QueueManager").start();
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

        // Add the job to the set of jobs to be run
        synchronized (jobExecuters) {
            jobsToRun.offer(job);

            // Start an executer for the job
            launchExecuter();
        }
    }

    /**
     * You need to hold the lock on {@link #jobExecuters} when running this
     * method.
     */
    private void launchExecuter() throws IOException {
        final JobExecuter executer =
                jobExecuterFactory.createJobExecuter(this, baseUrl);
        jobExecuters.put(executer.getExecuterId(), executer);
        executer.startExecuter();
    }

    @Override
    public Job getNextJob(final String executerId) {
        try {
            requireNonNull(executerId);
            final Job job = jobsToRun.take();
            executorJobId.put(executerId, job);
            logger.info(
                    "Executer " + executerId + " is running " + job.getId());
            queueManager.setJobRunning(job.getId());
            return job;
        } catch (final InterruptedException e) {
            return null;
        }
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
                + " chips or " + nBoards + " boards for " + (runTime / 1000.0)
                + " seconds");

        int nBoardsToRequest = nBoards;
        long quotaNCores = (long) (nBoards * CORES_PER_CHIP * CHIPS_PER_BOARD);

        // If nothing specified, use 3 boards
        if ((nBoards <= 0) && (nChips <= 0) && (nCores <= 0)) {
            nBoardsToRequest = 3;
            quotaNCores = (long) (3 * CORES_PER_CHIP * CHIPS_PER_BOARD);
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

            if ((ceil(nBoardsExact) - nBoardsExact) < 0.1) {
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
        final long resourceUsage = (long) ((runTime / 1000.0) * quotaNCores);
        logger.info("Resource usage " + resourceUsage);
        synchronized (jobResourceUsage) {
            jobResourceUsage.put(id, resourceUsage);
            jobNCores.put(id, quotaNCores);
        }
        addProvenance(id, Arrays.asList(new String[]{"spinnaker_machine"}),
                machine.getMachineName());

        return machine;
    }

    @Override
    public void releaseMachine(int id, String machineName) {
        synchronized (allocatedMachines) {
            final List<SpinnakerMachine> machines = allocatedMachines.get(id);
            if (machines != null) {
                int index = -1;
                SpinnakerMachine machine = null;
                for (int i = 0; i < machines.size(); i++) {
                    machine = machines.get(i);
                    if (machine.getMachineName() == machineName) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    machines.remove(index);
                    machineManager.releaseMachine(machine);
                }
            }
        }
    }

    @Override
    public void setMachinePower(int id, String machineName, boolean powerOn) {
        synchronized (allocatedMachines) {
            final List<SpinnakerMachine> machines = allocatedMachines.get(id);
            if (machines != null) {
                for (int i = 0; i < machines.size(); i++) {
                    SpinnakerMachine machine = machines.get(i);
                    if (machine.getMachineName() == machineName) {
                        machineManager.setMachinePower(machine, powerOn);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public ChipCoordinates getChipCoordinates(int id, String machineName,
            int chipX, int chipY) {
        synchronized (allocatedMachines) {
            final List<SpinnakerMachine> machines = allocatedMachines.get(id);
            if (machines != null) {
                for (int i = 0; i < machines.size(); i++) {
                    SpinnakerMachine machine = machines.get(i);
                    if (machine.getMachineName() == machineName) {
                        return machineManager.getChipCoordinates(machine,
                            chipX, chipY);
                    }
                }
            }
        }
        return null;
    }

    /** Get a machine to run the job on */
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
            usage = (long) (jobNCores.get(id) * (runTime / 1000.0));
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
        logger.debug("Updating log for " + id);
        logger.trace(id + ": " + logToAppend);
        queueManager.appendJobLog(id, requireNonNull(logToAppend));
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
        if (jobOutputTempFiles.containsKey(id)) {
            final File directory = jobOutputTempFiles.get(id);
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
                    logger.warn(
                        "Could not add provenance item " + path + " to job " +
                        id + ": Node " + item + " is not an object");
                    break;
                }
            }

            // If we can add the item, add it
            if (add) {
                current.put(path.get(path.size() - 1), value);
            }
        }
    }

    private ObjectNode getProvenance(final int id) {
        synchronized (jobProvenance) {
            return jobProvenance.remove(id);
        }
    }

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
        logger.debug("Marking job " + id + " as finished");
        releaseAllocatedMachines(id);

        // Do these before anything that can throw
        final long resourceUsage = getResourceUsage(id);
        final ObjectNode prov = getProvenance(id);

        try {
            queueManager.setJobFinished(id, logToAppend,
                    getOutputFiles(projectId, id, baseDirectory, outputs),
                    resourceUsage, prov);
        } catch (final IOException e) {
            logger.error("Error creating URLs while updating job", e);
        }
    }

    /** @return <tt>true</tt> if there were machines removed by this. */
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

        logger.debug("Marking job " + id + " as error");
        releaseAllocatedMachines(id);

        final Exception exception =
                reconstructRemoteException(error, stackTrace);
        // Do these before anything that can throw
        final long resourceUsage = getResourceUsage(id);
        final ObjectNode prov = getProvenance(id);

        try {
            queueManager.setJobError(id, logToAppend,
                    getOutputFiles(projectId, id, baseDirectory, outputs),
                    exception, resourceUsage, prov);
        } catch (final IOException e) {
            logger.error("Error creating URLs while updating job", e);
        }
    }

    private static final StackTraceElement[] STE_TMPL =
            new StackTraceElement[0];

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

    public void setExecutorExited(final String executorId,
            final String logToAppend) {
        final Job job = executorJobId.remove(requireNonNull(executorId));
        synchronized (jobExecuters) {
            jobExecuters.remove(executorId);
        }
        if (job != null) {
            final int id = job.getId();
            logger.debug("Job " + id + " has exited");

            if (releaseAllocatedMachines(id)) {
                logger.debug("Job " + id + " has not exited cleanly");
                try {
                    final long resourceUsage = getResourceUsage(id);
                    final ObjectNode prov = getProvenance(id);
                    final String projectId =
                            new File(job.getCollabId()).getName();
                    queueManager.setJobError(id, logToAppend,
                            getOutputFiles(projectId, id, null, null),
                            new Exception("Job did not finish cleanly"),
                            resourceUsage, prov);
                } catch (final IOException e) {
                    logger.error("Error creating URLs while updating job", e);
                }
            }
        } else {
            logger.error(
                    "An executer has exited.  This could indicate an error!");
            logger.error(logToAppend);

            if (restartJobExecuterOnFailure) {
                restartExecuters();
            }
        }
    }

    private void restartExecuters() {
        try {
            int jobSize;
            synchronized (jobsToRun) {
                jobSize = jobsToRun.size();
            }
            synchronized (jobExecuters) {
                while (jobSize > jobExecuters.size()) {
                    launchExecuter();
                }
            }
        } catch (final IOException e) {
            logger.error("Could not launch a new executer", e);
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
}
