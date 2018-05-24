package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

/**
 * A description of the state of a job, in terms of its state, whether its
 * boards are powered, the advised keep-alive polling interval and the reason
 * that the job died (if in the <tt>DESTROYED</tt> state).
 */
public class JobState {
    /** Job is unknown. */
    public static final int UNKNOWN = 0;
    /** Job is in the queue, awaiting allocation. */
    public static final int QUEUED = 1;
    /** Job is having its boards powered up. */
    public static final int POWER = 2;
    /** Job is running (or at least ready to run). */
    public static final int READY = 3;
    /** Job has terminated, see the <tt>reason</tt> property for why. */
    public static final int DESTROYED = 4;

    private int state;
    private Boolean power;
    private double keepAlive;
    private String reason;

    public int getState() {
        return state;
    }

    public void setState(final int state) {
        this.state = state;
    }

    public Boolean getPower() {
        return power;
    }

    public void setPower(final Boolean power) {
        this.power = power;
    }

    public double getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(final double keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }
}
