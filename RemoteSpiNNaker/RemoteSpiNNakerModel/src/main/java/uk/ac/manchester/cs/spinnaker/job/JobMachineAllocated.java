package uk.ac.manchester.cs.spinnaker.job;

/**
 * Indicates whether a machine was allocated for a job.
 */
public class JobMachineAllocated {
	private boolean allocated;

	/**
	 * Default constructor.
	 */
	public JobMachineAllocated() {
		this(false);
	}

	/**
	 * Create an instance.
	 *
	 * @param allocated
	 *            Whether the job was allocated.
	 */
	public JobMachineAllocated(boolean allocated) {
		this.allocated = allocated;
	}

	public boolean isAllocated() {
		return this.allocated;
	}

	public void setAllocated(boolean allocated) {
		this.allocated = allocated;
	}
}
