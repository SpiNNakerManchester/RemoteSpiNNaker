package uk.ac.manchester.cs.spinnaker.nmpi;

import java.io.IOException;

import uk.ac.manchester.cs.spinnaker.job.nmpi.Job;

/**
 * An interface for things that listen for new jobs.
 */
public interface NMPIQueueListener {
	/**
	 * Report that a new job has been presented to the queue.
	 * @param job
	 *            The job that's been reported to us by the queue.
	 * @return the ID of the executer handling the job
	 */
    String addJob(Job job) throws IOException;
}
