package uk.ac.manchester.cs.spinnaker.jobmanager;

import static java.io.File.createTempFile;
import static java.lang.Math.ceil;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.manchester.cs.spinnaker.job.JobMachineAllocated;
import uk.ac.manchester.cs.spinnaker.job.JobManagerInterface;
import uk.ac.manchester.cs.spinnaker.job.RemoteStackTrace;
import uk.ac.manchester.cs.spinnaker.job.RemoteStackTraceElement;
import uk.ac.manchester.cs.spinnaker.job.nmpi.DataItem;
import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;
import uk.ac.manchester.cs.spinnaker.machinemanager.MachineManager;
import uk.ac.manchester.cs.spinnaker.nmpi.NMPIQueueListener;
import uk.ac.manchester.cs.spinnaker.nmpi.NMPIQueueManager;
import uk.ac.manchester.cs.spinnaker.output.OutputManager;

/**
 * The manager of jobs; synchronises and manages all the ongoing and future
 * processes and machines.
 */
// TODO needs security; Role = JobEngine
public class JobManager implements NMPIQueueListener, JobManagerInterface {
	private static final double CHIPS_PER_BOARD = 48.0;
	private static final double CORES_PER_CHIP = 15.0;
	public static final String JOB_PROCESS_MANAGER_JAR = "RemoteSpiNNakerJobProcessManager.jar";

	private final MachineManager machineManager;
	private final NMPIQueueManager queueManager;
	private final OutputManager outputManager;
	private final URL baseUrl;
	private final JobExecuterFactory jobExecuterFactory;
	private final boolean restartJobExecuterOnFailure;

	private Log logger = LogFactory.getLog(getClass());
	private Map<Integer, List<SpinnakerMachine>> allocatedMachines = new HashMap<>();
	private Queue<Job> jobsToRun = new LinkedList<>();
	private Map<String, JobExecuter> jobExecuters = new HashMap<>();
	private Map<String, Job> executorJobId = new HashMap<String, Job>();
	private Map<Integer, File> jobOutputTempFiles = new HashMap<>();
	private Map<Integer, Long> jobNCores = new HashMap<>();
	private Map<Integer, Long> jobResourceUsage = new HashMap<>();
	private Map<Integer, Map<String, String>> jobProvenance = new HashMap<>();

	public JobManager(MachineManager machineManager,
			NMPIQueueManager queueManager, OutputManager outputManager,
			URL baseUrl, JobExecuterFactory jobExecuterFactory,
			boolean restartJobExecutorOnFailure) {
		this.machineManager = machineManager;
		this.queueManager = queueManager;
		this.outputManager = outputManager;
		this.baseUrl = baseUrl;
		this.jobExecuterFactory = jobExecuterFactory;
		this.restartJobExecuterOnFailure = restartJobExecutorOnFailure;

		// Start the queue manager
		queueManager.addListener(this);
		queueManager.start();
	}

	@Override
	public void addJob(Job job) throws IOException {
		logger.info("New job " + job.getId());

		// Add the job to the set of jobs to be run
		synchronized (jobExecuters) {
			synchronized (jobsToRun) {
				jobsToRun.add(job);
				jobsToRun.notifyAll();
			}

			// Start an executer for the job
			launchExecuter();
		}
	}

	/**
	 * You need to hold the lock on {@link #jobExecuters} when running this
	 * method.
	 */
	private void launchExecuter() throws IOException {
		JobExecuter executer = jobExecuterFactory.createJobExecuter(this,
				baseUrl);
		jobExecuters.put(executer.getExecuterId(), executer);
		executer.startExecuter();
	}

	@Override
	public Job getNextJob(String executerId) {
		synchronized (jobsToRun) {
			while (jobsToRun.isEmpty())
				try {
					jobsToRun.wait();
				} catch (InterruptedException e) {
					// Does Nothing
				}
			Job job = jobsToRun.poll();
			executorJobId.put(executerId, job);
			logger.info("Executer " + executerId + " is running " + job.getId());
			queueManager.setJobRunning(job.getId());
			return job;
		}
	}

	@Override
	public SpinnakerMachine getLargestJobMachine(int id, double runTime) {
		// TODO Check quota to get the largest machine within the quota

		SpinnakerMachine largest = null;
		for (SpinnakerMachine machine : machineManager.getMachines())
			if (largest == null || machine.getArea() > largest.getArea())
				largest = machine;

		return largest;
	}

	@Override
	public SpinnakerMachine getJobMachine(int id, int nCores, int nChips,
			int nBoards, double runTime) {
		// TODO Check quota

		logger.info("Request for " + nCores + " cores or " + nChips
				+ " chips or " + nBoards + " boards for " + (runTime / 1000.0)
				+ " seconds");

		int nBoardsToRequest = nBoards;
		long quotaNCores = (long) (nBoards * CORES_PER_CHIP * CHIPS_PER_BOARD);

		// If nothing specified, use 3 boards
		if (nBoards <= 0 && nChips <= 0 && nCores <= 0) {
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

			double nBoardsExact = (double) nChips / CHIPS_PER_BOARD;

			if (ceil(nBoardsExact) - nBoardsExact < 0.1)
				nBoardsExact += 1.0;
			if (nBoardsExact < 1.0)
				nBoardsExact = 1.0;
			nBoardsExact = ceil(nBoardsExact);
			nBoardsToRequest = (int) nBoardsExact;
		}

		// Get a machine to run the job on
		SpinnakerMachine machine = machineManager
				.getNextAvailableMachine(nBoardsToRequest);
		synchronized (allocatedMachines) {
			if (!allocatedMachines.containsKey(id))
				allocatedMachines.put(id, new ArrayList<SpinnakerMachine>());
			allocatedMachines.get(id).add(machine);
		}
		logger.info("Running " + id + " on " + machine.getMachineName());
		long resourceUsage = (long) ((runTime / 1000.0) * quotaNCores);
		logger.info("Resource usage " + resourceUsage);

		jobResourceUsage.put(id, resourceUsage);
		jobNCores.put(id, quotaNCores);

		return machine;
	}

	@Override
	public void extendJobMachineLease(int id, double runTime) {
		// TODO Check quota that the lease can be extended

		long usage = (long) (jobNCores.get(id) * (runTime / 1000.0));
		jobResourceUsage.put(id, usage);
		logger.info("Usage for " + id + " now " + usage);
	}

	@Override
	public JobMachineAllocated checkMachineLease(int id, int waitTime) {
		List<SpinnakerMachine> machines = null;
		synchronized (allocatedMachines) {
			machines = allocatedMachines.get(id);
		}

		// Return false if any machine is gone
		for (SpinnakerMachine machine : machines)
			if (!machineManager.isMachineAvailable(machine))
				return new JobMachineAllocated(false);

		// Wait for the state change of any machine
		waitForAnyMachineStateChange(waitTime, machines);

		// Again check for a machine which is gone
		for (SpinnakerMachine machine : machines)
			if (!machineManager.isMachineAvailable(machine))
				return new JobMachineAllocated(false);

		return new JobMachineAllocated(true);
	}

	private void waitForAnyMachineStateChange(final int waitTime,
			List<SpinnakerMachine> machines) {
		final Object stateChangeSync = new Object();
		for (final SpinnakerMachine machine : machines) {
			Thread stateThread = new Thread() {
				@Override
				public void run() {
					machineManager.waitForMachineStateChange(machine, waitTime);
					synchronized (stateChangeSync) {
						stateChangeSync.notify();
					}
				}
			};
			stateThread.start();
		}
		synchronized (stateChangeSync) {
			try {
				stateChangeSync.wait(waitTime);
			} catch (InterruptedException e) {
				// Does Nothing
			}
		}
	}

	@Override
	public void appendLog(int id, String logToAppend) {
		logger.debug("Updating log for " + id);
		logger.trace(id + ": " + logToAppend);
		queueManager.appendJobLog(id, logToAppend);
	}

	@Override
	public void addOutput(String projectId, int id, String output,
			InputStream input) {
		if (!jobOutputTempFiles.containsKey(id)) {
			try {
				File tempOutputDir = createTempFile("jobOutput", ".tmp");
				tempOutputDir.delete();
				tempOutputDir.mkdirs();
				jobOutputTempFiles.put(id, tempOutputDir);
			} catch (IOException e) {
				logger.error("Error creating temporary output directory for "
						+ id, e);
				throw new WebApplicationException(INTERNAL_SERVER_ERROR);
			}
		}

		File outputFile = new File(jobOutputTempFiles.get(id), output);
		try {
			outputFile.getParentFile().mkdirs();
			try (OutputStream outputStream = new FileOutputStream(outputFile)) {
				byte[] buffer = new byte[8096];
				int bytesRead = 0;
				while ((bytesRead = input.read(buffer)) >= 0)
					outputStream.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			logger.error("Error writing file " + outputFile + " for job " + id,
					e);
			throw new WebApplicationException(INTERNAL_SERVER_ERROR);
		}
	}

	private void getFilesRecursively(File directory, List<File> files) {
		for (File file : directory.listFiles())
			if (file.isDirectory()) {
				getFilesRecursively(file, files);
			} else {
				files.add(file);
			}
	}

	private List<DataItem> getOutputFiles(String projectId, int id,
			String baseFile, List<String> outputs) throws IOException {
		List<DataItem> outputItems = new ArrayList<>();
		if (outputs != null) {
			List<File> outputFiles = new ArrayList<>();
			for (String filename : outputs)
				outputFiles.add(new File(filename));
			outputItems.addAll(outputManager.addOutputs(projectId, id,
					new File(baseFile), outputFiles));
		}
		if (jobOutputTempFiles.containsKey(id)) {
			List<File> outputFiles = new ArrayList<>();
			File directory = jobOutputTempFiles.get(id);
			getFilesRecursively(directory, outputFiles);
			outputItems.addAll(outputManager.addOutputs(projectId, id,
					directory, outputFiles));
		}
		return outputItems;
	}

	@Override
	public void addProvenance(int id, String item, String value) {
		if (!jobProvenance.containsKey(id))
			jobProvenance.put(id, new HashMap<String, String>());
		jobProvenance.get(id).put(item, value);
	}

	@Override
	public void setJobFinished(String projectId, int id, String logToAppend,
			String baseDirectory, List<String> outputs) {
		logger.debug("Marking job " + id + " as finished");
		releaseAllocatedMachines(id);

		long resourceUsage = 0;
		if (jobResourceUsage.containsKey(id)) {
			resourceUsage = jobResourceUsage.remove(id);
			jobNCores.remove(id);
		}

		Map<String, String> provenance = jobProvenance.remove(id);

		try {
			queueManager.setJobFinished(id, logToAppend,
					getOutputFiles(projectId, id, baseDirectory, outputs),
					resourceUsage, provenance);
		} catch (IOException e) {
			logger.error("Error creating URLs while updating job", e);
		}
	}

	/** @return <tt>true</tt> if there were machines removed by this. */
	private boolean releaseAllocatedMachines(int id) {
		synchronized (allocatedMachines) {
			List<SpinnakerMachine> machines = allocatedMachines.remove(id);
			if (machines != null)
				for (SpinnakerMachine machine : machines)
					machineManager.releaseMachine(machine);
			return machines != null;
		}
	}

	@Override
	public void setJobError(String projectId, int id, String error,
			String logToAppend, String baseDirectory, List<String> outputs,
			RemoteStackTrace stackTrace) {
		logger.debug("Marking job " + id + " as error");
		releaseAllocatedMachines(id);

		Exception exception = new Exception(error);
		StackTraceElement[] elements = new StackTraceElement[stackTrace
				.getElements().size()];
		int i = 0;
		for (RemoteStackTraceElement element : stackTrace.getElements())
			elements[i++] = new StackTraceElement(element.getClassName(),
					element.getMethodName(), element.getFileName(),
					element.getLineNumber());
		exception.setStackTrace(elements);

		long resourceUsage = 0;
		if (jobResourceUsage.containsKey(id)) {
			resourceUsage = jobResourceUsage.remove(id);
			jobNCores.remove(id);
		}

		Map<String, String> provenance = jobProvenance.remove(id);

		try {
			queueManager.setJobError(id, logToAppend,
					getOutputFiles(projectId, id, baseDirectory, outputs),
					exception, resourceUsage, provenance);
		} catch (IOException e) {
			logger.error("Error creating URLs while updating job", e);
		}
	}

	public void setExecutorExited(String executorId, String logToAppend) {
		Job job = executorJobId.remove(executorId);
		jobExecuters.remove(executorId);
		if (job != null) {
			logger.debug("Job " + job.getId() + " has exited");

			if (releaseAllocatedMachines(job.getId())) {
				logger.debug("Job " + job.getId() + " has not exited cleanly");
				try {
					long resourceUsage = 0;
					if (jobResourceUsage.containsKey(job.getId())) {
						resourceUsage = jobResourceUsage.remove(job.getId());
						jobNCores.remove(job.getId());
					}

					Map<String, String> provenance = jobProvenance.remove(job
							.getId());

					String projectId = new File(job.getCollabId()).getName();
					queueManager.setJobError(job.getId(), logToAppend,
							getOutputFiles(projectId, job.getId(), null, null),
							new Exception("Job did not finish cleanly"),
							resourceUsage, provenance);
				} catch (IOException e) {
					logger.error("Error creating URLs while updating job", e);
				}
			}
		} else {
			logger.error("An executer has exited.  This could indicate an error!");
			logger.error(logToAppend);

			if (restartJobExecuterOnFailure)
				restartExecuters();
		}
	}

	private void restartExecuters() {
		try {
			int jobSize;
			synchronized (jobsToRun) {
				jobSize = jobsToRun.size();
			}
			synchronized (jobExecuters) {
				while (jobSize > jobExecuters.size())
					launchExecuter();
			}
		} catch (IOException e) {
			logger.error("Could not launch a new executer", e);
		}
	}

	@Override
	public Response getJobProcessManager() {
		InputStream jobManagerStream = getClass().getResourceAsStream(
				"/" + JOB_PROCESS_MANAGER_ZIP);
		if (jobManagerStream == null)
			throw new UnsatisfiedLinkError(JOB_PROCESS_MANAGER_ZIP
					+ " not found in classpath");
		return Response.ok(jobManagerStream).type(APPLICATION_ZIP).build();
	}
}
