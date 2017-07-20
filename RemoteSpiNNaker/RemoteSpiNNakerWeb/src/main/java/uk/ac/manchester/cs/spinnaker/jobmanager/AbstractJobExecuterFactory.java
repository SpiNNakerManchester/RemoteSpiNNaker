package uk.ac.manchester.cs.spinnaker.jobmanager;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class AbstractJobExecuterFactory implements JobExecuterFactory {
	private final Map<String, JobExecuter> map = new ConcurrentHashMap<>();

	/** Construct an instance of the executor for the given job URL */
	protected abstract JobExecuter makeExecuter(URL url) throws IOException;

	/** @see #makeExecuter(URL) */
	@Override
	public final JobExecuter createJobExecuter(URL baseUrl) throws IOException {
		requireNonNull(baseUrl);

		JobExecuter e = makeExecuter(baseUrl);
		addExecutor(e);
		return e;
	}

	/** Get the actual executer object for the ID. */
	@Override
	public JobExecuter getJobExecuter(String id) {
		// Note that this does *not* get from the database (necessarily)
		return map.get(id);
	}

	/** Store the executor in the executor cache. */
	protected void addExecutor(JobExecuter executor) {
		map.put(executor.getExecuterId(), executor);
	}

	/** Remove the executor from the executor cache. */
	protected void executorFinished(JobExecuter executor) {
		map.remove(executor.getExecuterId());
	}

	/** Count the number of entries in the executor cache. */
	protected int countExecuters() {
		return map.size();
	}
}
