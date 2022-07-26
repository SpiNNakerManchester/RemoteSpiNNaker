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
import static java.io.File.pathSeparator;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.FileUtils.copyToFile;
import static org.apache.commons.io.FileUtils.forceDeleteOnExit;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.IOUtils.buffer;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.cs.spinnaker.job.JobManagerInterface.JOB_PROCESS_MANAGER_ZIP;
import static uk.ac.manchester.cs.spinnaker.utils.ThreadUtils.waitfor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    private static final Logger logger = getLogger(Executer.class);

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
        if (isNull(jobManagerStream)) {
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
            for (ZipEntry entry = input.getNextEntry(); nonNull(entry);
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
    protected class Executer implements JobExecuter {

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
            new Thread(threadGroup, this::runSubprocess,
                    "Executer (" + id + ")").start();
        }

        private void runSubprocess() {
            try (JobOutputPipe pipe = startSubprocess(constructArguments())) {
                logger.debug("Waiting for process to finish");
                try {
                    process.waitFor();
                } catch (final InterruptedException e) {
                    // Do nothing; the thread will terminate shortly
                }
                logger.debug("Process finished, closing pipe");
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

            String classPath = jobProcessManagerClasspath.stream()
                    .map(File::toString).collect(joining(pathSeparator));
            command.add("-cp");
            command.add(classPath);
            logger.debug("Classpath: {}", classPath);

            command.add(JOB_PROCESS_MANAGER_MAIN_CLASS);
            logger.debug("Main command: {}", JOB_PROCESS_MANAGER_MAIN_CLASS);
            for (final String argument : arguments) {
                command.add(argument);
                logger.debug("Argument: {}", argument);
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
            logger.debug("Working directory: {}", jobExecuterDirectory);
            builder.redirectErrorStream(true);
            synchronized (this) {
                try {
                    logger.debug("Starting execution process");
                    process = builder.start();
                    logger.debug("Starting pipe from process");
                    JobOutputPipe pipe = new JobOutputPipe(
                            process.getInputStream(),
                            new PrintWriter(outputLog));
                    pipe.start();
                    return pipe;
                } catch (final IOException e) {
                    logger.error("Error running external job", e);
                    startException = e;
                    return null;
                } finally {
                    notifyAll();
                }
            }
        }

        /**
         * Report the results of the job using the log.
         */
        private void reportResult() {
            StringWriter loggedOutput = new StringWriter();
            try (FileReader reader = new FileReader(outputLog)) {
                copy(reader, loggedOutput);
            } catch (final IOException e) {
                logger.warn("problem in reporting log", e);
            }
            jobManager.setExecutorExited(id, loggedOutput.toString());
        }

        /**
         * Gets an OutputStream which writes to the process stdin.
         *
         * @return An OutputStream
         * @throws IOException If the output stream of the process can't be
         *     obtained
         */
        OutputStream getProcessOutputStream() throws IOException {
            synchronized (this) {
                while (isNull(process) && isNull(startException)) {
                    waitfor(this);
                }
                if (nonNull(startException)) {
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
        File getLogFile() {
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
            reader = buffer(new InputStreamReader(input));
            writer = output;
            done = false;
            setDaemon(true);
        }

        private String readLine() {
            try {
                return reader.readLine();
            } catch (final IOException e) {
                return null;
            }
        }

        @Override
        public void run() {
            try {
                String line;
                while (!done && nonNull(line = readLine())) {
                    if (!line.isEmpty()) {
                        logger.debug("{}", line);
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
