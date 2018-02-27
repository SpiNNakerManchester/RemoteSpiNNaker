package uk.ac.manchester.cs.spinnaker.job;

import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;

/**
 * The specification for a job. Can be any type of job, though the rest of this
 * implementation only really supports PyNN jobs running on SpiNNaker hardware
 * using sPyNNaker.
 */
public class JobSpecification {
	private SpinnakerMachine machine;
	private JobParameters parameters;
	private int id;
	private String url;

	/**
	 * Default constructor.
	 */
	public JobSpecification() {
		// Does Nothing
	}

	/**
	 * Create the specification for running a job.
	 *
	 * @param machine
	 *            The machine to run the job on.
	 * @param parameters
	 *            The parameters to the job.
	 * @param id
	 *            The ID of the job.
	 * @param url
	 *            The URL
	 */
	public JobSpecification(SpinnakerMachine machine, JobParameters parameters,
			int id, String url) {
		this.machine = machine;
		this.parameters = parameters;
		this.id = id;
		this.url = url;
	}

	public SpinnakerMachine getMachine() {
		return machine;
	}

	public void setMachine(SpinnakerMachine machine) {
		this.machine = machine;
	}

	public JobParameters getParameters() {
		return parameters;
	}

	public void setParameters(JobParameters parameters) {
		this.parameters = parameters;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
