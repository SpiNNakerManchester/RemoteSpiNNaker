package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

import java.util.Collection;

/**
 * A request to create a new job (i.e., an allocation of boards).
 * <p>
 * Jobs for which no suitable machines are available are immediately destroyed
 * (and the reason given).
 * <p>
 * Once a job has been created, it must be "kept alive" by a simple watchdog
 * mechanism. Jobs may be kept alive by periodically sending a "
 * {@linkplain JobKeepAliveCommand job_keepalive}" message or by sending any
 * other job-specific message. Jobs are culled if no keep alive message is
 * received for keepalive seconds. If absolutely necessary, a job’s keepalive
 * value may be set to <tt>null</tt>, disabling the keepalive mechanism.
 * <p>
 * Once a job has been allocated some boards, these boards will be automatically
 * powered on and left unbooted ready for use.
 * 
 * @author Jonathan Heathcote
 * @author Donal Fellows
 */
public class CreateJobCommand extends Command<Integer> {
	/*
	 * Not all allocation patterns are supported by this class; we don't support
	 * the default style or the triad-segment/specific-board styles.
	 * TODO Add in the other allocation patterns
	 */
	/**
	 * Create a command to ask spalloc to create a job. This will pick the first
	 * available machine of the given size.
	 * 
	 * @param n_boards
	 *            The minimum number of boards required.
	 * @param owner
	 *            The name of the owner of this job.
	 */
	public CreateJobCommand(int n_boards, String owner) {
		super("create_job");
		addArg(n_boards);
		addKwArg("owner", owner);
	}

	/**
	 * Create a command to ask spalloc to create a job. This will pick the
	 * machine with the given name, which must be tagged with "<tt>default</tt>".
	 * 
	 * @param n_boards
	 *            The minimum number of boards required.
	 * @param owner
	 *            The name of the owner of this job.
	 * @param machine
	 *            The name of a machine which this job must be executed on.
	 */
	public CreateJobCommand(int n_boards, String owner, String machine) {
		super("create_job");
		addArg(n_boards);
		addKwArg("owner", owner);
		addKwArg("machine", machine);
	}

	/**
	 * Create a command to ask spalloc to create a job. This will pick a machine
	 * that has all the given tags applied to it.
	 * 
	 * @param n_boards
	 *            The minimum number of boards required.
	 * @param owner
	 *            The name of the owner of this job.
	 * @param tags
	 *            The set of tags that the machine must have.
	 */
	public CreateJobCommand(int n_boards, String owner, Collection<String> tags) {
		super("create_job");
		addArg(n_boards);
		addKwArg("owner", owner);
		addKwArg("tags", tags.toArray());
	}

	/**
	 * The maximum number of seconds which may elapse between a query on this
	 * job before it is automatically destroyed. If <tt>null</tt>, no timeout is
	 * used. (Default: <tt>60.0</tt>)
	 */
	public void setKeepAlivePeriod(Float keepalive) {
		addKwArg("keepalive", keepalive);
	}

	/**
	 * The aspect ratio (h/w) which the allocated region must be "at least as
	 * square as". Set to <tt>0.0</tt> for any allowable shape, <tt>1.0</tt> to
	 * be exactly square. Ignored when allocating single boards or specific
	 * rectangles of triads.
	 */
	public void setMinRatio(float ratio) {
		addKwArg("min_ratio", ratio);
	}

	/**
	 * The maximum number of broken or unreachable boards to allow in the
	 * allocated region. If <tt>null</tt> (the default), any number of dead
	 * boards is permitted, as long as the board on the bottom-left corner is
	 * alive.
	 */
	public void setMaxDeadBoards(Integer max) {
		addKwArg("max_dead_boards", max);
	}

	/**
	 * The maximum number of broken links allow in the allocated region. When
	 * {@linkplain #setRequireTorus(boolean) require torus} is <tt>true</tt>
	 * this includes wrap-around links, otherwise peripheral links are not
	 * counted. If <tt>null</tt> (the default), any number of broken links is
	 * allowed.
	 */
	public void setMaxDeadLinks(Integer max) {
		addKwArg("max_dead_links", max);
	}

	/**
	 * If <tt>true</tt>, only allocate blocks with torus connectivity. In
	 * general this will only succeed for requests to allocate an entire machine
	 * (when the machine is otherwise not in use!). Must be <tt>false</tt> when
	 * allocating boards.
	 */
	public void setRequireTorus(boolean torusRequired) {
		addKwArg("require_torus", torusRequired);
	}
}
