package uk.ac.manchester.cs.spinnaker.jobmanager;

import static java.io.File.createTempFile;
import static java.io.File.pathSeparator;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.copyToFile;
import static org.apache.commons.io.FileUtils.forceDeleteOnExit;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.cs.spinnaker.job.JobManagerInterface.JOB_PROCESS_MANAGER_ZIP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

/**
 * An executer that runs its subprocesses on the local machine.
 */
public class LocalJobExecuterFactory implements JobExecuterFactory {

    /**
     * The job process manager main class.
     */
    private static final String JOB_PROCESS_MANAGER_MAIN_CLASS =
            "uk.ac.manchester.cs.spinnaker.jobprocessmanager.JobProcessManager";

    /**
     * Get the java executable.
     *
     * @return The java executable.
     * @throws IOException If the file can't be instantiated
     */
    private static File getJavaExec() throws IOException {
        final File binDir = new File(System.getProperty("java.home"), "bin");
        File exec = new File(binDir, "java");
        if (!exec.canExecute()) {
            exec = new File(binDir, "java.exe");
        }
        return exec;
    }

    /**
     * True if job files should be deleted on exit.
     */
    @Value("${deleteJobsOnExit}")
    private boolean deleteOnExit;

    /**
     * True if the job should live upload the logs.
     */
    @Value("${liveUploadOutput}")
    private boolean liveUploadOutput;

    /**
     * True if the job should request a SpiNNaker machine.
     */
    @Value("${requestSpiNNakerMachine}")
    private boolean requestSpiNNakerMachine;

    /**
     * A thread group for the executor.
     */
    private final ThreadGroup threadGroup;

    /**
     * The class path of the process manager.
     */
    private final List<File> jobProcessManagerClasspath = new ArrayList<>();

    /**
     * The directory in which the executor should start.
     */
    private File jobExecuterDirectory = null;

    /**
     * Logging.
     */
    private static Logger log = getLogger(Executer.class);

    /**
     * Create a new local executor.
     */
    public LocalJobExecuterFactory() {
        this.threadGroup = new ThreadGroup("LocalJob");
    }

    /**
     * Initialise the system state.
     *
     * @throws IOException
     *             if things go wrong.
     */
    @PostConstruct
    private void installJobExecuter() throws IOException {
        // Find the JobManager resource
        final InputStream jobManagerStream =
                getClass().getResourceAsStream("/" + JOB_PROCESS_MANAGER_ZIP);
        if (jobManagerStream == null) {
            throw new UnsatisfiedLinkError(
                    "/" + JOB_PROCESS_MANAGER_ZIP + " not found in classpath");
        }

        // Create a temporary folder
        jobExecuterDirectory = createTempFile("jobExecuter", "tmp");
        jobExecuterDirectory.delete();
        jobExecuterDirectory.mkdirs();
        jobExecuterDirectory.deleteOnExit();

        // Extract the JobManager resources
        try (ZipInputStream input = new ZipInputStream(jobManagerStream)) {
            for (ZipEntry entry = input.getNextEntry(); entry != null;
                    entry = input.getNextEntry()) {
                if (entry.isDirectory()) {
                    continue;
                }
                final File entryFile =
                        new File(jobExecuterDirectory, entry.getName());
                forceMkdirParent(entryFile);
                copyToFile(input, entryFile);
                forceDeleteOnExit(entryFile);

                if (entryFile.getName().endsWith(".jar")) {
                    jobProcessManagerClasspath.add(entryFile);
                }
            }
        }
    }

    /**
     * Create a new local execution.
     */
    @Override
    public JobExecuter createJobExecuter(final JobManager manager,
            final URL baseUrl) throws IOException {
        final String uuid = UUID.randomUUID().toString();
        final List<String> arguments = new ArrayList<>();
        arguments.add("--serverUrl");
        arguments.add(requireNonNull(baseUrl).toString());
        arguments.add("--local");
        arguments.add("--executerId");
        arguments.add(uuid);
        if (deleteOnExit) {
            arguments.add("--deleteOnExit");
        }
        if (liveUploadOutput) {
            arguments.add("--liveUploadOutput");
        }
        if (requestSpiNNakerMachine) {
            arguments.add("--requestMachine");
        }

        return new Executer(requireNonNull(manager), arguments, uuid);
    }

    /**
     * The executer thread.
     */
    class Executer implements JobExecuter, Runnable {

        /**
         * The job manager to report to.
         */
        private final JobManager jobManager;

        /**
         * The arguments to send to the command line.
         */
        private final List<String> arguments;

        /**
         * The ID of the executor.
         */
        private final String id;

        /**
         * The java executable to run.
         */
        private final File javaExec;

        /**
         * The output log file.
         */
        private final File outputLog = createTempFile("exec", ".log");

        /**
         * The executing external process.
         */
        private Process process;

        /**
         * Any exception discovered when starting the process.
         */
        private IOException startException;

        /**
         * Create a JobExecuter.
         *
         * @param jobManagerParam
         *            The job manager that wanted an executer made.
         * @param argumentsParam
         *            The arguments to use
         * @param idParam
         *            The id of the executer
         * @throws IOException
         *             If there is an error creating the log file
         */
        Executer(final JobManager jobManagerParam,
                final List<String> argumentsParam,
                final String idParam) throws IOException {
            this.jobManager = jobManagerParam;
            this.arguments = argumentsParam;
            this.id = idParam;
            javaExec = getJavaExec();
        }

        @Override
        public String getExecuterId() {
            return id;
        }

        @Override
        public void startExecuter() {
            new Thread(threadGroup, this, "Executer (" + id + ")").start();
        }

        /**
         * Runs the external job.
         *
         * @throws IOException
         *             If there is an error starting the job
         */
        @Override
        public void run() {
            try (JobOutputPipe pipe = startSubprocess(constructArguments())) {
                log.debug("Waiting for process to finish");
                try {
                    process.waitFor();
                } catch (final InterruptedException e) {
                    // Do nothing; the thread will terminate shortly
                }
                log.debug("Process finished, closing pipe");
            }

            reportResult();
        }

        /**
         * Construct the arguments from the class properties.
         *
         * @return The arguments as a list of strings.
         */
        private List<String> constructArguments() {
            final List<String> command = new ArrayList<>();
            command.add(javaExec.getAbsolutePath());

            final StringBuilder classPathBuilder = new StringBuilder();
            String separator = "";
            for (final File file : jobProcessManagerClasspath) {
                classPathBuilder.append(separator).append(file);
                separator = pathSeparator;
            }
            command.add("-cp");
            command.add(classPathBuilder.toString());
            log.debug("Classpath: " + classPathBuilder);

            command.add(JOB_PROCESS_MANAGER_MAIN_CLASS);
            log.debug("Main command: " + JOB_PROCESS_MANAGER_MAIN_CLASS);
            for (final String argument : arguments) {
                command.add(argument);
                log.debug("Argument: " + argument);
            }
            return command;
        }

        /**
         * Start executing the process.
         *
         * @param command The command and arguments
         * @return The output of the process as a pipe
         */
        private JobOutputPipe startSubprocess(final List<String> command) {
            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(jobExecuterDirectory);
            log.debug("Working directory: " + jobExecuterDirectory);
            builder.redirectErrorStream(true);
            JobOutputPipe pipe = null;
            synchronized (this) {
                try {
                    log.debug("Starting execution process");
                    process = builder.start();
                    log.debug("Starting pipe from process");
                    pipe = new JobOutputPipe(process.getInputStream(),
                            new PrintWriter(outputLog));
                    pipe.start();
                } catch (final IOException e) {
                    log.error("Error running external job", e);
                    startException = e;
                }
                notifyAll();
            }
            return pipe;
        }

        /**
         * Report the results of the job using the log.
         */
        private void reportResult() {
            final StringBuilder logToAppend = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(new FileReader(outputLog))) {
                while (true) {
                    final String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    logToAppend.append(line).append("\n");
                }
            } catch (final IOException e) {
                log.warn("problem in reporting log", e);
            }
            jobManager.setExecutorExited(id, logToAppend.toString());
        }

        /**
         * Gets an OutputStream which writes to the process stdin.
         *
         * @return An OutputStream
         * @throws IOException If the output stream of the process can't be
         *     obtained
         */
        public OutputStream getProcessOutputStream() throws IOException {
            synchronized (this) {
                while ((process == null) && (startException == null)) {
                    try {
                        wait();
                    } catch (final InterruptedException e) {
                        // Do Nothing
                    }
                }
                if (startException != null) {
                    throw startException;
                }
                return process.getOutputStream();
            }
        }

        /**
         * Gets the location of the process log file.
         *
         * @return The location of the log file
         */
        public File getLogFile() {
            return outputLog;
        }
    }

    /**
     * The pipe copier.
     */
    class JobOutputPipe extends Thread implements AutoCloseable {

        /**
         *  The input to the pipe.
         */
        private final BufferedReader reader;

        /**
         * The place where the output should be written.
         */
        private final PrintWriter writer;

        /**
         * True to stop execution.
         */
        private volatile boolean done;

        /**
         * Connect the input to the output.
         *
         * @param input
         *            Where things are coming from.
         * @param output
         *            Where things are going to. This class will close this when
         *            it is no longer required.
         */
        JobOutputPipe(final InputStream input, final PrintWriter output) {
            super(threadGroup, "JobOutputPipe");
            reader = new BufferedReader(new InputStreamReader(input));
            writer = output;
            done = false;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (!done) {
                    String line;
                    try {
                        line = reader.readLine();
                    } catch (final IOException e) {
                        break;
                    }
                    if (line == null) {
                        break;
                    }
                    if (!line.isEmpty()) {
                        log.debug(line);
                        writer.println(line);
                    }
                }
            } finally {
                writer.close();
            }
        }

        @Override
        public void close() {
            done = true;
            closeQuietly(reader);
        }
    }
}
