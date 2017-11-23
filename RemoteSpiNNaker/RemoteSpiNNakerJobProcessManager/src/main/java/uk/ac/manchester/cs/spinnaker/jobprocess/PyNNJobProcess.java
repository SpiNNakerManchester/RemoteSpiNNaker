package uk.ac.manchester.cs.spinnaker.jobprocess;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.IOUtils.closeQuietly;
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
import java.io.Reader;
import java.util.ArrayList;
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
import org.ini4j.ConfigParser;

import uk.ac.manchester.cs.spinnaker.job.Status;
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
     * The maximum error level expected.
     */
    private static final int MAX_ERROR_LEVEL = 128;

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

    /**
     * Executes the process.
     */
    @Override
    public void execute(final String machineUrl, final SpinnakerMachine machine,
            final PyNNJobParameters parameters, final LogWriter logWriter) {
        try {
            status = Running;
            workingDirectory = new File(parameters.getWorkingDirectory());

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
            if (exitValue >= MAX_ERROR_LEVEL) {
                // Useful to distinguish this case
                throw new Exception("Python exited with signal ("
                        + (exitValue - MAX_ERROR_LEVEL) + ")");
            }
            if (exitValue != 0) {
                throw new Exception("Python exited with a non-zero code ("
                        + exitValue + ")");
            }
            status = Finished;
        } catch (final Throwable e) {
            e.printStackTrace();
            error = e;
            status = Error;
        }
    }

    /**
     * Runs the sub process.
     *
     * @param parameters The parameters to execute the process with
     * @param logWriter A writer to write log messages to
     * @return The exit value of the process
     * @throws IOException If there was an error starting the process
     * @throws InterruptedException If the process was interrupted before return
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
     * Put the provenance data in to the local storage.
     *
     * @param items The hierarchy of items to be added
     * @param path The string path of the items
     * @param pathList The list path of the items
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
     * Add provenance items from a directory of xml files.
     *
     * @param provenanceDirectory The directory containing the files
     * @throws IOException If there was an error reading the files
     * @throws JAXBException If there was an error processing the files
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
     * Put the provenance data in to a zip file.
     *
     * @param reportsZip The zip file to put the data in
     * @param directory The directory containing the data
     * @param path The path to the directory
     * @throws IOException If there is an exception reading or writing files
     * @throws JAXBException If there is an exception processing files
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

    /**
     * Gather provenance files from the reports directory.
     *
     * @param workingDirectoryParam The process working directory
     * @throws IOException If there is an error reading the files
     * @throws JAXBException If there is an error processing the files
     */
    private void gatherProvenance(final File workingDirectoryParam)
            throws IOException, JAXBException {

        // Find the reports folder
        final File reportsFolder = new File(workingDirectoryParam, "reports");
        if (reportsFolder.isDirectory()) {

            // Create a zip file of the reports
            final ZipOutputStream reportsZip =
                    new ZipOutputStream(new FileOutputStream(
                            new File(workingDirectoryParam, "reports.zip")));

            // Gather items into the reports zip, keeping an eye out for
            // the "provenance data" folder
            zipProvenance(reportsZip, reportsFolder, "reports");
            reportsZip.close();
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
     * Transfers log messages from a read to a LogWriter.
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
         * @param readerParam The reader to read from
         * @param writerParam The writer to write to
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
         * @param input The input stream to read from.
         * @param writerParam The writer to write to.
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
