package uk.ac.manchester.cs.spinnaker.jobprocess;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.eclipse.jgit.api.Git.cloneRepository;
import static uk.ac.manchester.cs.spinnaker.job.Status.Error;
import static uk.ac.manchester.cs.spinnaker.job.Status.Finished;
import static uk.ac.manchester.cs.spinnaker.job.Status.Running;
import static uk.ac.manchester.cs.spinnaker.utils.Log.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.ini4j.ConfigParser;

import uk.ac.manchester.cs.spinnaker.job.Status;
import uk.ac.manchester.cs.spinnaker.job.pynn.PyNNHardwareConfiguration;
import uk.ac.manchester.cs.spinnaker.job.pynn.PyNNJobParameters;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;
import uk.ac.manchester.cs.spinnaker.utils.ThreadUtils;

/**
 * A process for running PyNN jobs.
 */
public class PyNNJobProcess implements JobProcess<PyNNJobParameters> {

    /**
     * The size of buffer to use when transferring data between files.
     */
    private static final int BUFFER_SIZE = 8196;

    /**
     * The error level that represents a signal.
     */
    private static final int MIN_SIGNAL_OFFSET = 128;

    /**
     * The directory containing provenance within the reports.
     */
    private static final String PROVENANCE_DIRECTORY = "provenance_data";

    /**
     * The section of the config where the machine is contained.
     */
    private static final String SECTION = "Machine";

    /**
     * The command to call to run the process.
     */
    private static final String SUBPROCESS_RUNNER = "python";

    /**
     * The time to wait for the process to finish.
     */
    private static final int FINALIZATION_DELAY = 1000;

    /**
     * The set of ignored file extensions in the outputs.
     */
    private static final Set<String> IGNORED_EXTENSIONS = new HashSet<>();

    /**
     * The set of ignored directories in the outputs.
     */
    private static final Set<String> IGNORED_DIRECTORIES = new HashSet<>();

    /**
     * A pattern for finding arguments of the command to execute.
     */
    private static final Pattern ARGUMENT_FINDER =
            Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
    static {
        IGNORED_EXTENSIONS.add("pyc");
        IGNORED_DIRECTORIES.add("application_generated_data_files");
        IGNORED_DIRECTORIES.add("reports");
    };

    /**
     * Provenance data items to be added to final provenance data.
     */
    private static final String[] PROVENANCE_ITEMS_TO_ADD = new String[]{
        "version_data/.*", "router_provenance/total_multi_cast_sent_packets",
        "router_provenance/total_created_packets",
        "router_provenance/total_dropped_packets",
        "router_provenance/total_missed_dropped_packets",
        "router_provenance/total_lost_dropped_packets"};

    private static final String GITHUB =
        "https://github.com/SpiNNakerManchester/";

    private static final String[] REPOSITORIES = new String[]{
        GITHUB + "SpiNNUtilities",
        GITHUB + "SpiNNStorageHandlers",
        GITHUB + "SpiNNMachine",
        GITHUB + "DataSpecification",
        GITHUB + "PACMAN",
        GITHUB + "SpiNNMan",
        GITHUB + "SpiNNFrontEndCommon",
        GITHUB + "sPyNNaker",
        GITHUB + "spinnaker_tools",
        GITHUB + "spinn_common"
    };

    private static final String[] PYNN_7_REPOSITORIES = new String[]{
        GITHUB + "sPyNNaker7"
    };

    private static final String[] PYNN_8_REPOSITORIES = new String[]{
        GITHUB + "sPyNNaker8"
    };

    private static final String[] GFE_REPOSITORIES = new String[]{
        GITHUB + "SpiNNakerGraphFrontEnd"
    };

    private static final String[] MAKE_DIRS = new String[]{
        "spinnaker_tools",
        "spinn_common",
        "SpiNNMan/c_models",
        "SpiNNFrontEndCommon/c_common",
        "sPyNNaker/neural_modelling"
    };

    private static final String[] PYTHON_SETUP_DIRS = new String[]{
        "SpiNNUtilities",
        "SpiNNStorageHandlers",
        "SpiNNMachine",
        "DataSpecification",
        "PACMAN",
        "SpiNNMan",
        "SpiNNFrontEndCommon",
        "sPyNNaker"
    };

    private static final String[] PYNN_7_SETUP_DIRS = new String[]{
        "sPyNNaker7"
    };

    private static final String[] PYNN_8_SETUP_DIRS = new String[]{
        "sPyNNaker8"
    };

    private static final String[] GFE_SETUP_DIRS = new String[]{

    };

    /**
     * The directory where the process is executed.
     */
    private File workingDirectory = null;

    /**
     * The current status of the process.
     */
    private Status status = null;

    /**
     * Any error that the process has exited with.
     */
    private Throwable error = null;

    /**
     * Output files from the process.
     */
    private final List<File> outputs = new ArrayList<>();

    /**
     * Provenance items of the process.
     */
    private final List<ProvenanceItem> provenance = new ArrayList<>();

    /**
     * A thread group for the log monitoring.
     */
    private ThreadGroup threadGroup;

    /**
     * Gathers files in a directory and sub-directories.
     *
     * @param directory The directory to find files in.
     * @return The set of files found.
     */
    private static Set<File> gatherFiles(final File directory) {
        return new LinkedHashSet<>(
                listFiles(directory, fileFilter(), directoryFilter()));
    }

    /**
     * A filter to remove files with ignored instructions.
     *
     * @return The file filter
     */
    private static IOFileFilter fileFilter() {
        return new AbstractFileFilter() {
            @Override
            public boolean accept(final File file) {
                return !IGNORED_EXTENSIONS
                        .contains(getExtension(file.getName()));
            }
        };
    }

    /**
     * A filter to remove ignored directories.
     *
     * @return The directory filter
     */
    private static IOFileFilter directoryFilter() {
        return new AbstractFileFilter() {
            @Override
            public boolean accept(final File file) {
                return !IGNORED_DIRECTORIES.contains(file.getName());
            }
        };
    }

    private void doGitClone(PyNNHardwareConfiguration config)
            throws InvalidRemoteException, TransportException, GitAPIException {
        List<String> allRepositories =
            new ArrayList<>(Arrays.asList(REPOSITORIES));
        if (config.getPyNNVersion().equals(
                PyNNHardwareConfiguration.PYNN_0_8)) {
            allRepositories.addAll(Arrays.asList(PYNN_8_REPOSITORIES));
        } else if (config.getPyNNVersion().equals(
                PyNNHardwareConfiguration.PYNN_0_7)) {
            allRepositories.addAll(Arrays.asList(PYNN_7_REPOSITORIES));
        } else {
            throw new RuntimeException(
                "Unknown " + PyNNHardwareConfiguration.PYNN_VERSION_KEY +
                ": " + config.getPyNNVersion());
        }
        for (String repo : allRepositories) {
            final CloneCommand clone = cloneRepository();
            clone.setURI(repo);
            clone.setDirectory(workingDirectory);
            clone.setCloneSubmodules(true);
            clone.setBranch(config.getSoftwareVersion());
            clone.call();
        }
    }

    /**
     * Read the output and put it in the log.
     * @param process The process to read
     * @param logWriter The writer to write to
     * @throws IOException If something goes wrong
     */
    private void readOutputToLog(final Process process,
            final LogWriter logWriter) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            logWriter.append(line);
            logWriter.append("\n");
            line = reader.readLine();
        }
    }

    private void run(File workingDir, LogWriter logWriter, String... command)
            throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
            command);
        processBuilder.directory(workingDir);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try {
            readOutputToLog(process, logWriter);
        } finally {
            process.destroy();
        }
    }

    public void doMake(PyNNHardwareConfiguration config, LogWriter logWriter)
            throws IOException {
        List<String> makeDirs = new ArrayList<>(Arrays.asList(MAKE_DIRS));
        makeDirs.addAll(Arrays.asList(config.getMakeDirs()));
        for (String dir : makeDirs) {
            File fileDir = new File(workingDirectory, dir);
            run(workingDirectory, logWriter, "make", "-C",
                    fileDir.getAbsolutePath());
            run(workingDirectory, logWriter, "make", "-C",
                    fileDir.getAbsolutePath(), "install");
        }
    }

    public void doPythonSetup(
            PyNNHardwareConfiguration config, LogWriter logWriter)
            throws IOException {
        List<String> setupDirs = new ArrayList<>(
            Arrays.asList(PYTHON_SETUP_DIRS));
        setupDirs.addAll(Arrays.asList(config.getPythonSetupDirs()));
        for (String dir : setupDirs) {
            File workingDir = new File(workingDirectory, dir);
            run(workingDir, logWriter, "python", "setup.py", "install",
                "--user");
        }
    }

    /**
     * Executes the process.
     */
    @Override
    public void execute(final String machineUrl, final SpinnakerMachine machine,
            final PyNNJobParameters parameters, final LogWriter logWriter) {
        try {
            status = Running;
            workingDirectory = new File(parameters.getWorkingDirectory());

            // Do git clone
            PyNNHardwareConfiguration config =
                parameters.getHardwareConfiguration();
            doGitClone(config);

            // Do make
            doMake(config, logWriter);

            // Do python setup
            doPythonSetup(config, logWriter);

            // TODO Deal with hardware configuration
            final File cfgFile = new File(workingDirectory, "spynnaker.cfg");

            // Add the details of the machine
            final ConfigParser parser = new ConfigParser();
            if (cfgFile.exists()) {
                parser.read(cfgFile);
            }

            if (!parser.hasSection(SECTION)) {
                parser.addSection(SECTION);
            }
            if (machine != null) {
                parser.set(SECTION, "machineName", machine.getMachineName());
                parser.set(SECTION, "version", machine.getVersion());
                parser.set(SECTION, "width", machine.getWidth());
                parser.set(SECTION, "height", machine.getHeight());
                final String bmpDetails = machine.getBmpDetails();
                if (bmpDetails != null) {
                    parser.set(SECTION, "bmp_names", bmpDetails);
                }
            } else {
                parser.set(SECTION, "remote_spinnaker_url", machineUrl);
            }
            parser.write(cfgFile);

            // Keep existing files to compare to later
            final Set<File> existingFiles = gatherFiles(workingDirectory);

            // Execute the program
            final int exitValue = runSubprocess(parameters, logWriter);

            // Get the provenance data
            gatherProvenance(workingDirectory);

            // Get any output files
            final Set<File> allFiles = gatherFiles(workingDirectory);
            for (final File file : allFiles) {
                if (!existingFiles.contains(file)) {
                    outputs.add(file);
                }
            }

            // If the exit is an error, mark an error
            if (exitValue >= MIN_SIGNAL_OFFSET) {
                // Useful to distinguish this case
                throw new Exception("Python exited with signal ("
                        + (exitValue - MIN_SIGNAL_OFFSET) + ")");
            }
            if (exitValue != 0) {
                throw new Exception("Python exited with a non-zero code ("
                        + exitValue + ")");
            }
            status = Finished;
        } catch (final Throwable e) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            logWriter.append(stringWriter.toString());
            e.printStackTrace();
            error = e;
            status = Error;
        }
    }

    /**
     * How to actually run a subprocess.
     *
     * @param parameters
     *            The parameters to the subprocess.
     * @param logWriter
     *            Where to send log messages.
     * @return
     *            The exit value of the process
     * @throws IOException
     *            If there was an error starting the process
     * @throws InterruptedException
     *            If the process was interrupted before return
     */
    private int runSubprocess(final PyNNJobParameters parameters,
            final LogWriter logWriter)
            throws IOException, InterruptedException {
        final List<String> command = new ArrayList<>();
        command.add(SUBPROCESS_RUNNER);

        final Matcher scriptMatcher =
                ARGUMENT_FINDER.matcher(parameters.getScript());
        while (scriptMatcher.find()) {
            command.add(
                    scriptMatcher.group(1).replace("{system}", "spiNNaker"));
        }

        final ProcessBuilder builder = new ProcessBuilder(command);
        log("Running " + command + " in " + workingDirectory);
        builder.directory(workingDirectory);
        builder.redirectErrorStream(true);
        final Process process = builder.start();

        // Run a thread to gather the log
        try (ReaderLogWriter logger =
                new ReaderLogWriter(process.getInputStream(), logWriter)) {
            logger.start();

            // Wait for the process to finish
            return process.waitFor();
        }
    }

    /**
     * Enter some provenance into the provenance map.
     *
     * @param items
     *            The items to insert.
     * @param path
     *            Where to insert these items relative to the current node, as a
     *            string.
     * @param pathList
     *            Where to insert these items relative to the current node, as a
     *            list.
     */
    private void putProvenanceInMap(final ProvenanceDataItems items,
            final String path, final LinkedList<String> pathList) {

        // Create a path for this level in the tree
        final String myPath = path + items.getName();
        pathList.addLast(items.getName());

        // Add all nested items
        for (final ProvenanceDataItems subItems : items
                .getProvenanceDataItems()) {
            putProvenanceInMap(subItems, myPath + "/", pathList);
        }

        // Add items from this level
        for (final ProvenanceDataItem subItem : items.getProvenanceDataItem()) {
            final String itemPath = myPath + "/" + subItem.getName();
            pathList.addLast(subItem.getName());
            for (final String item : PROVENANCE_ITEMS_TO_ADD) {
                if (itemPath.matches(item)) {
                    provenance.add(new ProvenanceItem(
                        new ArrayList<>(pathList), subItem.getValue()));
                }
            }
            pathList.removeLast();
        }
        pathList.removeLast();
    }

    /**
     * Add the provenance contained in the files in the given directory.
     *
     * @param provenanceDirectory
     *            Where to look for XML files.
     * @throws JAXBException
     *             If things go wrong in deserialisation.
     * @throws IOException
     *             If anything goes wrong with I/O.
     */
    private void addProvenance(final File provenanceDirectory)
            throws IOException, JAXBException {

        // Get provenance data from files
        for (final File file : provenanceDirectory.listFiles()) {

            // Only process XML files
            if (file.getName().endsWith(".xml")) {
                final JAXBContext jaxbContext =
                        JAXBContext.newInstance(ProvenanceDataItems.class);
                final Unmarshaller jaxbUnmarshaller =
                        jaxbContext.createUnmarshaller();
                final Source source = new StreamSource(file);
                final ProvenanceDataItems items =
                        (ProvenanceDataItems) jaxbUnmarshaller
                                .unmarshal(source);
                putProvenanceInMap(items, "", new LinkedList<String>());
            }
        }
    }

    /**
     * Used for creating a ZIP of the provenance.
     *
     * @param reportsZip
     *            Open handle to the ZIP being created.
     * @param directory
     *            Where to get provenance data from.
     * @param path
     *            The path within the ZIP.
     * @throws IOException
     *             If anything goes wrong with I/O.
     * @throws JAXBException
     *             If anything goes wrong with deserialisation of the XML.
     */
    private void zipProvenance(final ZipOutputStream reportsZip,
            final File directory, final String path)
            throws IOException, JAXBException {

        // Go through the report files and zip them up
        final byte[] buffer = new byte[BUFFER_SIZE];
        for (final File file : directory.listFiles()) {
            if (file.isDirectory()) {
                zipProvenance(reportsZip, file, path + "/" + file.getName());

                // If the directory is the provenance directory, process it
                if (file.getName().equals(PROVENANCE_DIRECTORY)) {
                    addProvenance(file);
                }
            } else {
                final ZipEntry entry =
                        new ZipEntry(path + "/" + file.getName());
                reportsZip.putNextEntry(entry);
                try (FileInputStream in = new FileInputStream(file)) {
                    int bytesRead = in.read(buffer);
                    while (bytesRead >= 0) {
                        reportsZip.write(buffer, 0, bytesRead);
                        bytesRead = in.read(buffer);
                    }
                }
            }
        }
    }

    /**
     * Gather the provenance information from the job's reports directory.
     *
     * @param workingDirectoryParam
     *            The job's working directory.
     * @throws IOException
     *             If anything goes wrong with I/O.
     * @throws JAXBException
     *             If anything goes wrong with deserialisation of XML.
     */
    private void gatherProvenance(final File workingDirectoryParam)
            throws IOException, JAXBException {

        // Find the reports folder
        final File reportsFolder = new File(workingDirectoryParam, "reports");
        if (reportsFolder.isDirectory()) {

            // Create a zip file of the reports
            try (ZipOutputStream reportsZip =
                    new ZipOutputStream(new FileOutputStream(
                            new File(workingDirectoryParam, "reports.zip")))) {
                // Gather items into the reports zip, keeping an eye out for
                // the "provenance data" folder
                zipProvenance(reportsZip, reportsFolder, "reports");
            }
        }
    }

    /**
     * Get the status of the process.
     */
    @Override
    public Status getStatus() {
        return status;
    }

    /**
     * Get the process error.
     */
    @Override
    public Throwable getError() {
        return error;
    }

    /**
     * Get the outputs of the process.
     */
    @Override
    public List<File> getOutputs() {
        return outputs;
    }

    /**
     * Get the provenance of the process.
     */
    @Override
    public List<ProvenanceItem> getProvenance() {
        return provenance;
    }

    /**
     * Clean up the process after exit.
     */
    @Override
    public void cleanup() {
        // Does Nothing
    }

    /**
     * Thread for copying a {@link Reader} to a {@link LogWriter}.
     */
    class ReaderLogWriter extends Thread implements AutoCloseable {

        /**
         * The reader to read from.
         */
        private final BufferedReader reader;

        /**
         * The writer to write to.
         */
        private final LogWriter writer;

        /**
         * True when running, False to stop.
         */
        private boolean running;

        /**
         * Creates a new ReaderLogWriter with another reader.
         *
         * @param readerParam
         *            The reader to read from
         * @param writerParam
         *            The writer to write to
         */
        ReaderLogWriter(
                final Reader readerParam, final LogWriter writerParam) {
            super(threadGroup, "Reader Log Writer");
            requireNonNull(readerParam);
            if (readerParam instanceof BufferedReader) {
                this.reader = (BufferedReader) readerParam;
            } else {
                this.reader = new BufferedReader(readerParam);
            }
            this.writer = requireNonNull(writerParam);
            setDaemon(true);
        }

        /**
         * Creates a new ReaderLogWriter with an input stream. This will be
         * treated as a text stream using the system encoding.
         *
         * @param input
         *            The input stream to read from.
         * @param writerParam
         *            The writer to write to.
         */
        ReaderLogWriter(final InputStream input,
                final LogWriter writerParam) {
            this(new InputStreamReader(input), writerParam);
        }

        @Override
        public void run() {
            try {
                copyStream();
            } catch (IOException | RuntimeException e) {
                return;
            } finally {
                synchronized (this) {
                    running = false;
                    notifyAll();
                }
            }
        }

        @Override
        public void start() {
            running = true;
            super.start();
        }

        /**
         * Perform the copying of the stream.
         *
         * @throws IOException If there is an error copying
         */
        private void copyStream() throws IOException {
            while (!interrupted()) {
                final String line = reader.readLine();
                if (line == null) {
                    return;
                }
                writer.append(line + "\n");
            }
        }

        /**
         * Closes the reader/writer.
         */
        @Override
        public void close() {
            log("Waiting for log writer to exit...");

            synchronized (this) {
                try {
                    while (running) {
                        wait();
                    }
                } catch (final InterruptedException e) {
                    // Does Nothing
                }
            }

            log("Log writer has exited");
            closeQuietly(reader);
            ThreadUtils.sleep(FINALIZATION_DELAY);
        }
    }
}
