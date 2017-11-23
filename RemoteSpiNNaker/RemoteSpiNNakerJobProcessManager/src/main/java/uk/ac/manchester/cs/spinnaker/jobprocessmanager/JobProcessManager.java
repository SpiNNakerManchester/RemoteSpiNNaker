package uk.ac.manchester.cs.spinnaker.jobprocessmanager;

import static java.lang.String.format;
import static java.lang.System.exit;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.eclipse.jgit.util.FileUtils.createTempDir;
import static uk.ac.manchester.cs.spinnaker.jobprocessmanager.RemoteSpiNNakerAPI.createJobManager;
import static uk.ac.manchester.cs.spinnaker.utils.FileDownloader.downloadFile;
import static uk.ac.manchester.cs.spinnaker.utils.Log.log;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import uk.ac.manchester.cs.spinnaker.job.JobManagerInterface;
import uk.ac.manchester.cs.spinnaker.job.JobParameters;
import uk.ac.manchester.cs.spinnaker.job.RemoteStackTrace;
import uk.ac.manchester.cs.spinnaker.job.Status;
import uk.ac.manchester.cs.spinnaker.job.nmpi.DataItem;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job.pynn.PyNNJobParameters;
import uk.ac.manchester.cs.spinnaker.job_parameters.JobParametersFactory;
import uk.ac.manchester.cs.spinnaker.job_parameters.JobParametersFactoryException;
import uk.ac.manchester.cs.spinnaker.jobprocess.JobProcess;
import uk.ac.manchester.cs.spinnaker.jobprocess.JobProcessFactory;
import uk.ac.manchester.cs.spinnaker.jobprocess.LogWriter;
import uk.ac.manchester.cs.spinnaker.jobprocess.ProvenanceItem;
import uk.ac.manchester.cs.spinnaker.jobprocess.PyNNJobProcess;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

/**
 * Manages a running job process. This is run as a separate process from the
 * command line, and it assumes input is passed via {@link System#in}.
 */
public class JobProcessManager {

    /**
     * The interval at which the log is updated.
     */
    private static final int UPDATE_INTERVAL = 500;

    /** The factory for converting parameters into processes. */
    private static final JobProcessFactory JOB_PROCESS_FACTORY =
            new JobProcessFactory("JobProcess");
    static {
        JOB_PROCESS_FACTORY.addMapping(PyNNJobParameters.class,
                PyNNJobProcess.class);
    }

    /**
     * A log writer that uploads the log at a fixed time since the last change
     * was made.
     */
    class UploadingJobManagerLogWriter extends JobManagerLogWriter {

        /**
         * The timer for the interval.
         */
        private final Timer sendTimer;

        /**
         * Create a new uploading writer.
         */
        UploadingJobManagerLogWriter() {
            sendTimer = new Timer(UPDATE_INTERVAL, new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    sendLog();
                }
            });
        }

        /**
         * Send the log now if changed.
         */
        private void sendLog() {
            String toWrite = null;
            synchronized (this) {
                if (isPopulated()) {
                    toWrite = getLog();
                    resetCache();
                }
            }
            if (toWrite != null) {
                log("Sending cached data to job manager");
                jobManager.appendLog(job.getId(), toWrite);
            }
        }

        @Override
        public void append(final String logMsg) {
            log("Process Output: " + logMsg);
            synchronized (this) {
                appendCache(logMsg);
                sendTimer.restart();
            }
        }

        @Override
        public void stop() {
            sendTimer.stop();
        }
    }

    /**
     * The URL of the Job Manager server.
     */
    private final String serverUrl;

    /**
     * True if the working directory should be cleaned on exit.
     */
    private final boolean deleteOnExit;

    /**
     * True if the process is running on the same machine as the server.
     */
    private final boolean isLocal;

    /**
     * The ID of the execution.
     */
    private final String executerId;

    /**
     * True if the output should be uploaded as it is produced.
     */
    private final boolean liveUploadOutput;

    /**
     * True if a machine should be requested for the job.
     */
    private final boolean requestMachine;

    /**
     * The authorisation token of this job on the server.
     */
    private final String authToken;

    /**
     * The connection to the Job Manager.
     */
    private JobManagerInterface jobManager;

    /**
     * The writer of the log.
     */
    private JobManagerLogWriter logWriter;

    /**
     * The job being executed.
     */
    private Job job;

    /**
     * The ID of the project in which the job exists.
     */
    private String projectId;

    /**
     * Creates a manager of a Job Process.
     *
     * @param serverUrlParam The URL of the server
     * @param deleteOnExitParam True if the job output should be deleted
     * @param isLocalParam True if the job is running on the same machine as
     *     the server
     * @param executerIdParam The ID of the job execution
     * @param liveUploadOutputParam True if the output of the job should be live
     *     uploaded to the server
     * @param requestMachineParam True if a machine should be requested for the
     *     job
     * @param authTokenParam The authorisation token of the job
     */
    public JobProcessManager(final String serverUrlParam,
            final boolean deleteOnExitParam, final boolean isLocalParam,
            final String executerIdParam, final boolean liveUploadOutputParam,
            final boolean requestMachineParam, final String authTokenParam) {
        this.serverUrl = requireNonNull(
                serverUrlParam, "--serverUrl must be specified");
        this.executerId = requireNonNull(
                executerIdParam, "--executerId must be specified");
        this.deleteOnExit = deleteOnExitParam;
        this.isLocal = isLocalParam;
        this.liveUploadOutput = liveUploadOutputParam;
        this.requestMachine = requestMachineParam;
        this.authToken = authTokenParam;
    }

    /**
     * Runs the job.
     */
    public void runJob() {
        try {
            jobManager = createJobManager(serverUrl, authToken);

            // Read the job
            job = jobManager.getNextJob(executerId);
            projectId = new File(job.getCollabId()).getName();

            // Create a temporary location for the job
            final File workingDirectory = createTempDir("job", ".tmp", null);

            final JobParameters parameters = getJobParameters(workingDirectory);

            // Create a process to process the request
            log("Creating process from parameters");
            final JobProcess<JobParameters> process =
                    JOB_PROCESS_FACTORY.createProcess(parameters);
            logWriter = getLogWriter();

            // Get a machine
            SpinnakerMachine machine = null;
            String machineUrl = null;
            if (requestMachine) {
                machine = jobManager.getJobMachine(job.getId(), DEFAULT,
                        DEFAULT, DEFAULT, DEFAULT);
            } else {
                machineUrl = format("%sjob/%d/machine", serverUrl, job.getId());
            }

            // Execute the process
            log("Running job " + job.getId() + " on " + machine + " using "
                    + parameters.getClass() + " reporting to " + serverUrl);
            process.execute(machineUrl, machine, parameters, logWriter);
            logWriter.stop();

            // Get the exit status
            processOutcome(workingDirectory, process, logWriter.getLog());
        } catch (final Exception error) {
            reportFailure(error);
            exit(1);
        }
    }

    /**
     * Report a job failure.
     *
     * @param error The error of the failure.
     */
    private void reportFailure(final Throwable error) {
        if ((jobManager == null) || (job == null)) {
            log(error);
            return;
        }

        try {
            String log = "";
            if (logWriter != null) {
                logWriter.stop();
                log = logWriter.getLog();
            }
            String message = error.getMessage();
            if (message == null) {
                message = "No Error Message";
            }
            jobManager.setJobError(projectId, job.getId(), message, log, "",
                    new ArrayList<String>(), new RemoteStackTrace(error));
        } catch (final Throwable t) {
            // Exception while reporting exception...
            log(t);
            log(error);
            exit(2);
        }
    }

    /**
     * Main Method.
     *
     * @param args Main arguments
     * @throws Exception If anything goes wrong!
     */
    public static void main(final String[] args) throws Exception {
        String serverUrl = null;
        boolean deleteOnExit = false;
        boolean isLocal = false;
        String executerId = null;
        boolean liveUploadOutput = false;
        boolean requestMachine = false;
        String authToken = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--serverUrl" :
                    serverUrl = args[++i];
                    break;
                case "--executerId" :
                    executerId = args[++i];
                    break;
                case "--deleteOnExit" :
                    deleteOnExit = true;
                    break;
                case "--local" :
                    isLocal = true;
                    break;
                case "--liveUploadOutput" :
                    liveUploadOutput = true;
                    break;
                case "--requestMachine" :
                    requestMachine = true;
                    break;
                case "--authToken" :
                    try (BufferedReader r = new BufferedReader(
                            new InputStreamReader(System.in))) {
                        authToken = r.readLine();
                    }
                    break;
                default :
                    throw new IllegalArgumentException(
                            "unknown option: " + args[i]);
            }
        }

        new JobProcessManager(serverUrl, deleteOnExit, isLocal, executerId,
                liveUploadOutput, requestMachine, authToken).runJob();
        exit(0);
    }

    /**
     * The default value for setting up a machine.
     */
    private static final int DEFAULT = -1;

    /**
     * Sort out the parameters to a job. Includes downloading any necessary
     * files.
     *
     * @param workingDirectory
     *            The working directory for the job, used to write files.
     * @return Description of the parameters.
     * @throws IOException
     *             If anything goes wrong, such as the parameters being
     *             unreadable or the job being unsupported on the current
     *             architectural configuration.
     */
    private JobParameters getJobParameters(final File workingDirectory)
            throws IOException {
        final Map<String, JobParametersFactoryException> errors =
                new HashMap<>();
        final JobParameters parameters = JobParametersFactory
                .getJobParameters(job, workingDirectory, errors);

        if (parameters == null) {
            if (!errors.isEmpty()) {
                throw new JobErrorsException(errors);
            }
            // Miscellaneous other error
            throw new IOException(
                    "The job did not appear to be supported on this system");
        }

        // Get any requested input files
        if (job.getInputData() != null) {
            for (final DataItem input : job.getInputData()) {
                downloadFile(input.getUrl(), workingDirectory, null);
            }
        }

        return parameters;
    }

    /**
     * Get the log writer.
     *
     * @return The log writer
     */
    private JobManagerLogWriter getLogWriter() {
        if (!liveUploadOutput) {
            return new SimpleJobManagerLogWriter();
        }
        return new UploadingJobManagerLogWriter();
    }

    /**
     * Process the outcome of the job execution.
     *
     * @param workingDirectory The directory where the job was run
     * @param process The process of the job
     * @param log The log message of the job
     * @throws IOException If there is an error reading or writing files
     */
    private void processOutcome(final File workingDirectory,
            final JobProcess<?> process, final String log)
            throws IOException {
        final Status status = process.getStatus();
        log("Process has finished with status " + status);

        final List<File> outputs = process.getOutputs();
        final List<String> outputsAsStrings = new ArrayList<>();
        for (final File output : outputs) {
            if (isLocal) {
                outputsAsStrings.add(output.getAbsolutePath());
            } else {
                try (InputStream input = new FileInputStream(output)) {
                    jobManager.addOutput(projectId, job.getId(),
                            output.getName(), input);
                }
            }
        }

        for (final ProvenanceItem item : process.getProvenance()) {
            jobManager.addProvenance(
                job.getId(), item.getPath(), item.getValue());
        }

        switch (status) {
            case Error :
                final Throwable error = process.getError();
                String message = error.getMessage();
                if (message == null) {
                    message = "No Error Message";
                }
                jobManager.setJobError(projectId, job.getId(), message, log,
                        workingDirectory.getAbsolutePath(), outputsAsStrings,
                        new RemoteStackTrace(error));
                break;
            case Finished :
                jobManager.setJobFinished(projectId, job.getId(), log,
                        workingDirectory.getAbsolutePath(), outputsAsStrings);

                // Clean up
                process.cleanup();
                if (deleteOnExit) {
                    deleteQuietly(workingDirectory);
                }
                break;
            default :
                throw new IllegalStateException("Unknown status returned!");
        }
    }
}

/**
 * Job writer that writes logs to the JobManager.
 */
abstract class JobManagerLogWriter implements LogWriter {

    /**
     * The cached message.
     */
    private final StringBuilder cached = new StringBuilder();

    /**
     * Adds a message to the cache.
     *
     * @param message The message to add
     */
    protected synchronized void appendCache(final String message) {
        cached.append(message);
    }

    /**
     * Reset the cache to contain nothing.
     */
    protected synchronized void resetCache() {
        cached.setLength(0);
    }

    /**
     * Determine if the cache has any data in it.
     *
     * @return True if there is data, False otherwise.
     */
    protected synchronized boolean isPopulated() {
        return cached.length() > 0;
    }

    /**
     * Get the current content of the cache.
     * @return The current content of the log.
     */
    public synchronized String getLog() {
        return cached.toString();
    }

    /**
     * Stop the log.
     */
    void stop() {
    }
}

/**
 *
 */
class SimpleJobManagerLogWriter extends JobManagerLogWriter {
    @Override
    public void append(final String logMsg) {
        log("Process Output: " + logMsg);
        synchronized (this) {
            appendCache(logMsg);
        }
    }
}

/**
 * An exception in the job.
 */
@SuppressWarnings("serial")
class JobErrorsException extends IOException {

    /**
     * The error message of the exception.
     */
    private static final String MAIN_MSG = "The job type was recognised"
            + " by at least one factory, but could not be decoded.  The"
            + " errors are as follows:";

    /**
     * Builds an error message from a map of errors.
     *
     * @param errors The errors to use.
     * @return An exception containing the errors.
     */
    private static String
            buildMessage(final Map<String, ? extends Exception> errors) {
        StringWriter buffer = new StringWriter();
        PrintWriter bufferWriter = new PrintWriter(buffer);
        bufferWriter.println(MAIN_MSG);
        for (String key : errors.keySet()) {
            bufferWriter.print(key);
            bufferWriter.println(":");
            errors.get(key).printStackTrace(bufferWriter);
            bufferWriter.println();
        }
        return buffer.toString();
    }

    /**
     * Creates an exception from a set of errors.
     *
     * @param errors The errors to build the exception from
     */
    JobErrorsException(
            final Map<String, JobParametersFactoryException> errors) {
        super(buildMessage(errors));
    }
}
