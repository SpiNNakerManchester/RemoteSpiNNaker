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
package uk.ac.manchester.cs.spinnaker.jobprocessmanager;

import static java.lang.String.format;
import static java.lang.System.exit;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.io.File.createTempFile;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.IOUtils.buffer;
import static org.eclipse.jgit.util.FileUtils.createTempDir;
import static uk.ac.manchester.cs.spinnaker.jobprocessmanager.RemoteSpiNNakerAPI.createJobManager;
import static uk.ac.manchester.cs.spinnaker.utils.FileDownloader.downloadFile;
import static uk.ac.manchester.cs.spinnaker.utils.Log.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Timer;

import uk.ac.manchester.cs.spinnaker.job.JobManagerInterface;
import uk.ac.manchester.cs.spinnaker.job.RemoteStackTrace;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.job_parameters.JobParameters;
import uk.ac.manchester.cs.spinnaker.job_parameters.JobParametersFactory;
import uk.ac.manchester.cs.spinnaker.job_parameters.JobParametersFactoryException;
import uk.ac.manchester.cs.spinnaker.job_parameters.PyNNJobParameters;
import uk.ac.manchester.cs.spinnaker.jobprocess.JobProcess;
import uk.ac.manchester.cs.spinnaker.jobprocess.JobProcessFactory;
import uk.ac.manchester.cs.spinnaker.jobprocess.LogWriter;
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

    /**
     * The maximum size of the log before the log is simply saved to a file.
     */
    private static final int MAX_LOG_STREAMED = 10000;

    /**
     * Default parameters for getting a machine.
     */
    private static final int DEFAULT = -1;

    /**
     * The factory for converting parameters into processes.
     */
    private static final JobProcessFactory JOB_PROCESS_FACTORY =
            new JobProcessFactory("JobProcess");
    static {
        JOB_PROCESS_FACTORY.addMapping(PyNNJobParameters.class,
                PyNNJobProcess::new);
    }

    /**
     * A log writer that uploads to the server.
     */
    class UploadingJobManagerLogWriter extends JobManagerLogWriter {

        /**
         * The timer for the interval.
         */
        private final Timer sendTimer;

        /**
         * The size of the log so far.
         */
        private int logSize;

        /**
         * The file that the log is being written to locally.
         */
        private final File logFile;

        /**
         * The log file to store logs in as well.
         */
        private final FileWriter logFileWriter;

        /**
         * Make a log writer that uploads the log every half second.
         * @throws IOException If there is an error creating a log file.
         */
        UploadingJobManagerLogWriter() throws IOException {
            sendTimer = new Timer(UPDATE_INTERVAL, e -> sendLog());
            logFile = createTempFile("output_", ".txt", workingDirectory);
            logFileWriter = new FileWriter(logFile);
            log("Output will be written to " + logFile.getName());
        }

        /**
         * Send the log now if changed.
         */
        private void sendLog() {
            synchronized (this) {
                String toWrite = null;
                if (isPopulated()) {
                    toWrite = takeCache();
                }
                if (nonNull(toWrite) && !toWrite.isEmpty()) {
                    try {
                        jobManager.appendLog(job.getId(), toWrite);
                    } catch (Throwable e) {
                        log("Error sending log");
                        log(e);
                    }
                }
            }
        }

        @Override
        public void append(final String logMsg) throws IOException {
            logFileWriter.append(logMsg);
            logFileWriter.flush();
            if (logSize < MAX_LOG_STREAMED) {
                int nextSize = logSize + logMsg.length();
                if (nextSize < MAX_LOG_STREAMED) {
                    // The log is still small enough after adding the new
                    // message, add it to the streamed data
                    synchronized (this) {
                        sendTimer.restart();
                        appendCache(logMsg);
                    }
                } else {
                    // The log has only just become too big so report it now
                    appendCache("Output has become too large to be streamed."
                        + " The rest of the log will be available in "
                        + logFile.getName()
                        + " when the run has completed.");
                    // Still send things that have been done up to this point
                    synchronized (this) {
                        sendTimer.stop();
                        sendLog();
                    }
                }

                // We can update in the if statement as we don't need this once
                // it is too big
                logSize = nextSize;
            }
        }

        @Override
        public void stop() {
            synchronized (this) {
                sendTimer.stop();
            }
            try {
                logFileWriter.close();
            } catch (IOException e) {
                log("Error closing log file");
                log(e);
            }
        }

        /**
         * Get the log file written to.
         * @return The log file written to.
         */
        public File getLogFile() {
            return logFile;
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
     * The working directory of the job.
     */
    private File workingDirectory;

    /**
     * Create an object that manages the running of a single job.
     *
     * @param serverUrlParam
     *            The URL to the server, used for writing back results.
     * @param deleteOnExitParam
     *            Whether to delete the job's resources on termination.
     * @param isLocalParam
     *            Whether the job is local.
     * @param executerIdParam
     *            The ID of the executer.
     * @param liveUploadOutputParam
     *            Whether to do live upload of output data.
     * @param requestMachineParam
     *            Whether to request a machine.
     * @param authTokenParam
     *            The authorisation token for the server.
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
        this.liveUploadOutput = liveUploadOutputParam;
        this.requestMachine = requestMachineParam;
        this.authToken = authTokenParam;
    }

    /**
     * Run a single job.
     */
    public void runJob() {
        try {
            jobManager = createJobManager(serverUrl, authToken);

            // Read the job
            job = jobManager.getNextJob(executerId);
            projectId = new File(job.getCollabId()).getName();

            // Create a temporary location for the job
            workingDirectory = createTempDir("job", ".tmp", null);

            // Download the setup script
            var downloadUrl = serverUrl + JobManagerInterface.PATH
                    + "/" + JobManagerInterface.SETUP_SCRIPT;
            log("Downloading setup script from " + downloadUrl);
            final var setupScript = downloadFile(downloadUrl,
                    workingDirectory, JobManagerInterface.SETUP_SCRIPT);

            final var parameters = getJobParameters(
                    setupScript.getAbsolutePath());

            // Create a process to process the request
            log("Creating process from parameters");
            final var process = JOB_PROCESS_FACTORY.createProcess(parameters);
            logWriter = getLogWriter();

            // Read the machine
            final var machine = getMachine();

            // Execute the process
            log("Running job " + job.getId() + " on " + machine + " using "
                    + parameters.getClass() + " reporting to " + serverUrl);
            process.execute(machine.getURL(), machine.getMachine(), parameters,
                    logWriter);
            logWriter.stop();

            // Get the exit status
            processOutcome(process, logWriter.getLog());
        } catch (final Exception error) {
            log(error);
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
        if (isNull(jobManager) || isNull(job)) {
            log(error);
            return;
        }

        try {
            var log = "";
            if (nonNull(logWriter)) {
                logWriter.stop();
                log = logWriter.getLog();
            }
            var message = error.getMessage();
            if (isNull(message)) {
                message = "No Error Message";
            }
            jobManager.setJobError(job.getId(), message, log,
                    new RemoteStackTrace(error));
        } catch (final Throwable t) {
            // Exception while reporting exception...
            log(t);
            log(error);
            exit(2);
        }
    }

    /**
     * How to run a Job Process Manager. This is the execution entry point for
     * this Maven module.
     *
     * @param args
     *            The command line arguments.
     * @throws Exception
     *             No guarantees made about what can go wrong.
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
                try (var r = buffer(
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
     * Get a machine for use in the job.
     *
     * @return a machine
     */
    private Machine getMachine() {
        // (get a 3 board machine just now)
        if (requestMachine) {
            return new Machine(jobManager.getJobMachine(job.getId(), DEFAULT,
                    DEFAULT, DEFAULT, DEFAULT));
        }
        return new Machine(serverUrl, job.getId());
    }

    /**
     * Sort out the parameters to a job. Includes downloading any necessary
     * files.
     *
     * @param setupScript
     *            The setup script
     * @return Description of the parameters.
     * @throws IOException
     *             If anything goes wrong, such as the parameters being
     *             unreadable or the job being unsupported on the current
     *             architectural configuration.
     */
    private JobParameters getJobParameters(final String setupScript)
            throws IOException {
        final var errors = new HashMap<String, JobParametersFactoryException>();
        final var parameters = JobParametersFactory.getJobParameters(
                job, workingDirectory, setupScript, errors);

        if (isNull(parameters)) {
            if (!errors.isEmpty()) {
                throw new JobErrorsException(errors);
            }
            // Miscellaneous other error
            throw new IOException(
                    "The job did not appear to be supported on this system");
        }

        workingDirectory = parameters.getWorkingDirectory();

        // Get any requested input files
        if (nonNull(job.getInputData())) {
            for (final var input : job.getInputData()) {
                downloadFile(input.getUrl(), workingDirectory, null);
            }
        }

        return parameters;
    }

    /**
     * Get the log writer.
     *
     * @return The log writer
     * @throws IOException if there is an error opening the log writer
     */
    private JobManagerLogWriter getLogWriter() throws IOException {
        if (!liveUploadOutput) {
            return new SimpleJobManagerLogWriter();
        }
        return new UploadingJobManagerLogWriter();
    }

    /**
     * Process the outcome of the job execution.
     *
     * @param process The process of the job
     * @param log The log message of the job
     * @throws IOException If there is an error reading or writing files
     */
    private void processOutcome(final JobProcess<?> process, final String log)
            throws IOException {
        final var status = process.getStatus();
        log("Process has finished with status " + status);

        final var outputs = process.getOutputs();
        if (logWriter instanceof UploadingJobManagerLogWriter) {
            outputs.add(((UploadingJobManagerLogWriter) logWriter)
                    .getLogFile());
        }
        for (final var output : outputs) {
            try (var input = new FileInputStream(output)) {
                jobManager.addOutput(projectId, job.getId(),
                        workingDirectory.getAbsolutePath(),
                        output.getAbsolutePath(), input);
            }
        }

        for (final var item : process.getProvenance()) {
            jobManager.addProvenance(
                job.getId(), item.getPath(), item.getValue());
        }

        switch (status) {
        case Error :
            final var error = process.getError();
            var message = error.getMessage();
            if (isNull(message)) {
                message = "No Error Message";
            }
            jobManager.setJobError(job.getId(), message, log,
                    new RemoteStackTrace(error));
            break;
        case Finished :
            jobManager.setJobFinished(job.getId(), log);

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
 * A description of a machine.
 */
class Machine {
    /** The machine. Knows its service URL. */
    private SpinnakerMachine machine;
    /** The service URL. */
    private String url;

    /**
     * Create a machine known by object.
     *
     * @param machineParam
     *            The machine object.
     */
    Machine(final SpinnakerMachine machineParam) {
        this.machine = machineParam;
    }

    /**
     * Create a machine known by service job name.
     *
     * @param baseUrl
     *            The base URL for the machine.
     * @param id
     *            The ID for the job.
     */
    Machine(final String baseUrl, final int id) {
        this.url = format("%sjob/%d/machine", baseUrl, id);
    }

    /**
     * Get the machine.
     *
     * @return the machine
     */
    SpinnakerMachine getMachine() {
        return machine;
    }

    /**
     * Get the URL.
     *
     * @return the URL
     */
    String getURL() {
        return url;
    }

    @Override
    public String toString() {
        if (nonNull(machine)) {
            return machine.toString();
        }
        return url;
    }
}

/**
 * How to write to the log.
 */
abstract class JobManagerLogWriter implements LogWriter {

    /**
     * Create a machine known by object.
     *
     * @param machine
     *            The machine object.
     */
    /**
     * The cached message.
     */
    private final StringBuilder cached = new StringBuilder();


    /**
     * Does the log have anything in it?
     *
     * @return Whether the log cache is non-empty.
     */
    protected synchronized boolean isPopulated() {
        return cached.length() > 0;
    }

    /**
     * Adds a message to the cache.
     *
     * @param message The message to add
     */
    protected synchronized void appendCache(final String message) {
        cached.append(message);
    }

    /**
     * Get the current contents of the log cache.
     *
     * @return Copy of the log contents.
     */
    public synchronized String getLog() {
        return cached.toString();
    }

    /**
     * Get the current log contents and reset the internal buffer.
     *
     * @return The contents of the log prior to this call.
     */
    public final synchronized String takeCache() {
        try {
            return cached.toString();
        } finally {
            cached.setLength(0);
        }
    }

    /**
     * Get the number of characters in the current log.
     *
     * @return The number of characters in the log.
     */
    public final int cacheSize() {
        return cached.length();
    }

    /**
     * Stop any background activity associated with the log.
     */
    void stop() {
    }
}

/**
 * A simple log implementation.
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
 * Exception indicating errors with the job factory.
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
        var buffer = new StringWriter();
        var bufferWriter = new PrintWriter(buffer);
        bufferWriter.println(MAIN_MSG);
        for (final var key : errors.keySet()) {
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
