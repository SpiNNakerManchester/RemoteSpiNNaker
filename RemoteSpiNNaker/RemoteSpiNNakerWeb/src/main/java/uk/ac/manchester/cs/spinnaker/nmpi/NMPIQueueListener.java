package uk.ac.manchester.cs.spinnaker.nmpi;

import java.io.IOException;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;

/**
 * An interface for things that listen for new jobs.
 */
public interface NMPIQueueListener {
	/**
	 * Adds a job to the listener.
	 *
	 * @param job
	 *            The job to add.
	 * @throws IOException
	 *             If anything goes wrong.
	 */
	void addJob(Job job) throws IOException;
}
