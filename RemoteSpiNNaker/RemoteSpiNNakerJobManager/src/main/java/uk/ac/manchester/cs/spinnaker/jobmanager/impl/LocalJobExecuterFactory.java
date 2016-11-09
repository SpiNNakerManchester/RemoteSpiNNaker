package uk.ac.manchester.cs.spinnaker.jobmanager.impl;

import static java.io.File.createTempFile;
import static org.apache.commons.io.FileUtils.copyToFile;
import static org.apache.commons.io.FileUtils.forceDeleteOnExit;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static uk.ac.manchester.cs.spinnaker.job.JobManagerInterface.JOB_PROCESS_MANAGER_ZIP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import uk.ac.manchester.cs.spinnaker.jobmanager.JobExecuter;
import uk.ac.manchester.cs.spinnaker.jobmanager.JobExecuterFactory;
import uk.ac.manchester.cs.spinnaker.jobmanager.JobManager;

public class LocalJobExecuterFactory implements JobExecuterFactory {
	private static final String JOB_PROCESS_MANAGER_MAIN_CLASS = "uk.ac.manchester.cs.spinnaker.jobprocessmanager.JobProcessManager";

	private static File getJavaExec() throws IOException {
		File binDir = new File(System.getProperty("java.home"), "bin");
		File exec = new File(binDir, "java");
		if (!exec.canExecute())
			exec = new File(binDir, "java.exe");
		return exec;
	}

	private final boolean deleteOnExit;
	private final boolean liveUploadOutput;
	private final boolean requestSpiNNakerMachine;

	private List<File> jobProcessManagerClasspath = new ArrayList<>();
	private File jobExecuterDirectory = null;

	public LocalJobExecuterFactory(boolean deleteOnExit,
			boolean liveUploadOutput, boolean requestSpiNNakerMachine)
			throws IOException {
		this.deleteOnExit = deleteOnExit;
		this.liveUploadOutput = liveUploadOutput;
		this.requestSpiNNakerMachine = requestSpiNNakerMachine;

		// Create a temporary folder
		jobExecuterDirectory = createTempFile("jobExecuter", "tmp");
		jobExecuterDirectory.delete();
		jobExecuterDirectory.mkdirs();
		jobExecuterDirectory.deleteOnExit();

		// Find the JobManager resource
		InputStream jobManagerStream = getClass().getResourceAsStream(
				"/" + JOB_PROCESS_MANAGER_ZIP);
		if (jobManagerStream == null)
			throw new UnsatisfiedLinkError("/" + JOB_PROCESS_MANAGER_ZIP
					+ " not found in classpath");

		// Extract the JobManager resources
		try (ZipInputStream input = new ZipInputStream(jobManagerStream)) {
			for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input
					.getNextEntry()) {
				if (entry.isDirectory())
					continue;
				File entryFile = new File(jobExecuterDirectory, entry.getName());
				forceMkdirParent(entryFile);
				copyToFile(input, entryFile);
				forceDeleteOnExit(entryFile);

				if (entryFile.getName().endsWith(".jar"))
					jobProcessManagerClasspath.add(entryFile);
			}
		}
	}

	@Override
	public JobExecuter createJobExecuter(JobManager manager, URL baseUrl)
			throws IOException {
		String uuid = UUID.randomUUID().toString();
		List<String> arguments = new ArrayList<>();
		arguments.add("--serverUrl");
		arguments.add(baseUrl.toString());
		arguments.add("--local");
		arguments.add("--executerId");
		arguments.add(uuid);
		if (deleteOnExit)
			arguments.add("--deleteOnExit");
		if (liveUploadOutput)
			arguments.add("--liveUploadOutput");
		if (requestSpiNNakerMachine)
			arguments.add("--requestMachine");

		return new LocalJobExecuter(manager, getJavaExec(),
				jobProcessManagerClasspath, JOB_PROCESS_MANAGER_MAIN_CLASS,
				arguments, jobExecuterDirectory, uuid);
	}
}
