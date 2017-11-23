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
 * A process for running PyNN jobs
 */
public class PyNNJobProcess implements JobProcess<PyNNJobParameters> {
    private static final String PROVENANCE_DIRECTORY = "provenance_data";
    private static final String SECTION = "Machine";
    private static final String SUBPROCESS_RUNNER = "python";
    private static final int FINALIZATION_DELAY = 1000;
    private static final Set<String> IGNORED_EXTENSIONS = new HashSet<>();
    private static final Set<String> IGNORED_DIRECTORIES = new HashSet<>();
    private static final Pattern ARGUMENT_FINDER =
            Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
    static {
        IGNORED_EXTENSIONS.add("pyc");
        IGNORED_DIRECTORIES.add("application_generated_data_files");
        IGNORED_DIRECTORIES.add("reports");
    };
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
        "sPyNNaker",
        "sPyNNaker7",
        "sPyNNaker8"
    };

    private File workingDirectory = null;
    private Status status = null;
    private Throwable error = null;
    private final List<File> outputs = new ArrayList<>();
    private final List<ProvenanceItem> provenance = new ArrayList<>();
    ThreadGroup threadGroup;

    private static Set<File> gatherFiles(final File directory) {
        return new LinkedHashSet<>(
                listFiles(directory, fileFilter(), directoryFilter()));
    }

    private static IOFileFilter fileFilter() {
        return new AbstractFileFilter() {
            @Override
            public boolean accept(final File file) {
                return !IGNORED_EXTENSIONS
                        .contains(getExtension(file.getName()));
            }
        };
    }

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

    private void readOutputToLog(Process process, LogWriter logWriter)
            throws IOException {
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

            // TODO: Deal with hardware configuration
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
            if (exitValue > 127) {
                // Useful to distinguish this case
                throw new Exception("Python exited with signal ("
                        + (exitValue - 128) + ")");
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

    /** How to actually run a subprocess. */
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

    private void zipProvenance(final ZipOutputStream reportsZip,
            final File directory, final String path)
            throws IOException, JAXBException {

        // Go through the report files and zip them up
        final byte[] buffer = new byte[8196];
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
                final FileInputStream in = new FileInputStream(file);
                int bytesRead = in.read(buffer);
                while (bytesRead >= 0) {
                    reportsZip.write(buffer, 0, bytesRead);
                    bytesRead = in.read(buffer);
                }
                in.close();
            }
        }
    }

    private void gatherProvenance(final File workingDirectory)
            throws IOException, JAXBException {

        // Find the reports folder
        final File reportsFolder = new File(workingDirectory, "reports");
        if (reportsFolder.isDirectory()) {

            // Create a zip file of the reports
            final ZipOutputStream reportsZip =
                    new ZipOutputStream(new FileOutputStream(
                            new File(workingDirectory, "reports.zip")));

            // Gather items into the reports zip, keeping an eye out for
            // the "provenance data" folder
            zipProvenance(reportsZip, reportsFolder, "reports");
            reportsZip.close();
        }
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public Throwable getError() {
        return error;
    }

    @Override
    public List<File> getOutputs() {
        return outputs;
    }

    @Override
    public List<ProvenanceItem> getProvenance() {
        return provenance;
    }

    @Override
    public void cleanup() {
        // Does Nothing
    }

    class ReaderLogWriter extends Thread implements AutoCloseable {
        private final BufferedReader reader;
        private final LogWriter writer;

        private boolean running;

        /**
         * Creates a new ReaderLogWriter with another reader.
         *
         * @param reader
         *            The reader to read from
         * @param writer
         *            The writer to write to
         */
        public ReaderLogWriter(final Reader reader, final LogWriter writer) {
            super(threadGroup, "Reader Log Writer");
            requireNonNull(reader);
            if (reader instanceof BufferedReader) {
                this.reader = (BufferedReader) reader;
            } else {
                this.reader = new BufferedReader(reader);
            }
            this.writer = requireNonNull(writer);
            setDaemon(true);
        }

        /**
         * Creates a new ReaderLogWriter with an input stream. This will be
         * treated as a text stream using the system encoding.
         *
         * @param input
         *            The input stream to read from.
         * @param writer
         *            The writer to write to.
         */
        public ReaderLogWriter(final InputStream input,
                final LogWriter writer) {
            this(new InputStreamReader(input), writer);
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
         * Closes the reader/writer
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
