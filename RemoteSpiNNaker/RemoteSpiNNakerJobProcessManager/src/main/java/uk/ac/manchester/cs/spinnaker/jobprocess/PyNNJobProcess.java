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
	private static final String PROVENANCE_DIRECTORY = "provenance_data";
	private static final String SECTION = "Machine";
	private static final String SUBPROCESS_RUNNER = "python";
	private static final int FINALIZATION_DELAY = 1000;
	private static final Set<String> IGNORED_EXTENSIONS = new HashSet<>();
	private static final Set<String> IGNORED_DIRECTORIES = new HashSet<>();
	private static final Pattern ARGUMENT_FINDER = Pattern
			.compile("([^\"]\\S*|\".+?\")\\s*");
	static {
		IGNORED_EXTENSIONS.add("pyc");
		IGNORED_DIRECTORIES.add("application_generated_data_files");
		IGNORED_DIRECTORIES.add("reports");
	};
	private static final String[] PROVENANCE_ITEMS_TO_ADD = new String[] {
			"version_data/.*",
			"router_provenance/total_multi_cast_sent_packets",
			"router_provenance/total_created_packets",
			"router_provenance/total_dropped_packets",
			"router_provenance/total_missed_dropped_packets",
			"router_provenance/total_lost_dropped_packets" };
	private static final int SIGNAL_EXIT_OFFSET = 128;
	/**
	 * Used for deserialising provenance data. We build this exactly once
	 * because it is comparatively expensive and the resulting object is thread
	 * safe.
	 *
	 * @see https://stackoverflow.com/a/7400735/301832
	 */
	static final JAXBContext JAXB_CONTEXT;

	static {
		// Set up the JAXB (deserialisation) context.
		try {
			JAXB_CONTEXT = JAXBContext.newInstance(ProvenanceDataItems.class);
		} catch (JAXBException e) {
			throw new RuntimeException("unexpected JAXB failure", e);
		}
	}

	private File workingDirectory = null;
	private Status status = null;
	private Throwable error = null;
	private final List<File> outputs = new ArrayList<>();
	private final List<ProvenanceItem> provenance = new ArrayList<>();
	private volatile ThreadGroup threadGroup;

	private static Set<File> gatherFiles(File directory) {
		return new LinkedHashSet<>(
				listFiles(directory, fileFilter(), directoryFilter()));
	}

	private static IOFileFilter fileFilter() {
		return new AbstractFileFilter() {
			@Override
			public boolean accept(File file) {
				return !IGNORED_EXTENSIONS
						.contains(getExtension(file.getName()));
			}
		};
	}

	private static IOFileFilter directoryFilter() {
		return new AbstractFileFilter() {
			@Override
			public boolean accept(File file) {
				return !IGNORED_DIRECTORIES.contains(file.getName());
			}
		};
	}

	@Override
	public void execute(String machineUrl, SpinnakerMachine machine,
			PyNNJobParameters parameters, LogWriter logWriter) {
		try {
			status = Running;
			workingDirectory = new File(parameters.getWorkingDirectory());

			// TODO: Deal with hardware configuration
			File cfgFile = new File(workingDirectory, "spynnaker.cfg");

			// Add the details of the machine
			ConfigParser parser = new ConfigParser();
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
				String bmpDetails = machine.getBmpDetails();
				if (bmpDetails != null) {
					parser.set(SECTION, "bmp_names", bmpDetails);
				}
			} else {
				parser.set(SECTION, "remote_spinnaker_url", machineUrl);
			}
			parser.write(cfgFile);

			// Keep existing files to compare to later
			Set<File> existingFiles = gatherFiles(workingDirectory);

			// Execute the program
			int exitValue = runSubprocess(parameters, logWriter);

			// Get the provenance data
			gatherProvenance(workingDirectory);

			// Get any output files
			Set<File> allFiles = gatherFiles(workingDirectory);
			for (File file : allFiles) {
				if (!existingFiles.contains(file)) {
					outputs.add(file);
				}
			}

			// If the exit is an error, mark an error
			if (exitValue >= SIGNAL_EXIT_OFFSET) {
				// Useful to distinguish this case
				throw new Exception("Python exited with signal ("
						+ (exitValue - SIGNAL_EXIT_OFFSET) + ")");
			}
			if (exitValue != 0) {
				throw new Exception("Python exited with a non-zero code ("
						+ exitValue + ")");
			}
			status = Finished;
		} catch (Throwable e) {
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
	 */
	private int runSubprocess(PyNNJobParameters parameters, LogWriter logWriter)
			throws IOException, InterruptedException {
		List<String> command = new ArrayList<>();
		command.add(SUBPROCESS_RUNNER);

		Matcher scriptMatcher = ARGUMENT_FINDER.matcher(parameters.getScript());
		while (scriptMatcher.find()) {
			command.add(
					scriptMatcher.group(1).replace("{system}", "spiNNaker"));
		}

		ProcessBuilder builder = new ProcessBuilder(command);
		log("Running " + command + " in " + workingDirectory);
		builder.directory(workingDirectory);
		builder.redirectErrorStream(true);
		Process process = builder.start();

		// Run a thread to gather the log
		try (ReaderLogWriter logger = new ReaderLogWriter(
				process.getInputStream(), logWriter)) {
			logger.start();

			// Wait for the process to finish
			return process.waitFor();
		}
	}

	/**
	 * Enter some provenance into the provenance map relative to the root of the
	 * map.
	 *
	 * @param items
	 *            The items to insert.
	 */
	private void putProvenanceInMap(ProvenanceDataItems items) {
		putProvenanceInMap(items, "", new LinkedList<String>());
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
	private void putProvenanceInMap(ProvenanceDataItems items, String path,
			LinkedList<String> pathList) {
		// Create a path for this level in the tree
		String myPath = path + items.getName();
		pathList.addLast(items.getName());

		// Add all nested items
		for (ProvenanceDataItems subItems : items.getProvenanceDataItems()) {
			putProvenanceInMap(subItems, myPath + "/", pathList);
		}

		// Add items from this level
		for (ProvenanceDataItem subItem : items.getProvenanceDataItem()) {
			String itemPath = myPath + "/" + subItem.getName();
			pathList.addLast(subItem.getName());
			for (String item : PROVENANCE_ITEMS_TO_ADD) {
				if (itemPath.matches(item)) {
					provenance.add(new ProvenanceItem(new ArrayList<>(pathList),
							subItem.getValue()));
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
	 */
	private void addProvenance(File provenanceDirectory) throws JAXBException {
		Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
		// Get provenance data from files
		for (File file : provenanceDirectory.listFiles()) {
			// Only process XML files
			if (file.getName().endsWith(".xml")) {
				putProvenanceInMap(
						unmarshaller.unmarshal(new StreamSource(file),
								ProvenanceDataItems.class).getValue());
			}
		}
	}

	private static final int CHUNK_SIZE = 8196;

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
	private void zipProvenance(ZipOutputStream reportsZip, File directory,
			String path) throws IOException, JAXBException {
		// Go through the report files and zip them up
		byte[] buffer = new byte[CHUNK_SIZE];
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				zipProvenance(reportsZip, file, path + "/" + file.getName());

				// If the directory is the provenance directory, process it
				if (file.getName().equals(PROVENANCE_DIRECTORY)) {
					addProvenance(file);
				}
			} else {
				ZipEntry entry = new ZipEntry(path + "/" + file.getName());
				reportsZip.putNextEntry(entry);
				FileInputStream in = new FileInputStream(file);
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
	 * Gather the provenance information from the job's reports directory.
	 *
	 * @param workingDirectory
	 *            The job's working directory.
	 * @throws IOException
	 *             If anything goes wrong with I/O.
	 * @throws JAXBException
	 *             If anything goes wrong with deserialisation of XML.
	 */
	private void gatherProvenance(File workingDirectory)
			throws IOException, JAXBException {
		// Find the reports folder
		File reportsFolder = new File(workingDirectory, "reports");
		if (reportsFolder.isDirectory()) {
			// Create a zip file of the reports
			try (ZipOutputStream reportsZip = new ZipOutputStream(
					new FileOutputStream(
							new File(workingDirectory, "reports.zip")))) {
				// Gather items into the reports zip, keeping an eye out for
				// the "provenance data" folder
				zipProvenance(reportsZip, reportsFolder, "reports");
			}
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

	/**
	 * Thread for copying a {@link Reader} to a {@link LogWriter}.
	 */
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
		ReaderLogWriter(Reader reader, LogWriter writer) {
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
		ReaderLogWriter(InputStream input, LogWriter writer) {
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
				String line = reader.readLine();
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
				} catch (InterruptedException e) {
					// Does Nothing
				}
			}

			log("Log writer has exited");
			closeQuietly(reader);
			ThreadUtils.sleep(FINALIZATION_DELAY);
		}
	}
}
