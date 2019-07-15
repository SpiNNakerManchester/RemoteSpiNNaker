/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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

    /**
     * The state of the job in the queue (from the above list).
     */
    private int state;

    /**
     * The power state of the job.
     */
    private Boolean power;

    /**
     * The number of seconds to keep alive.
     */
    private double keepAlive;

    /**
     * The reason for the job being destroyed.
     */
    private String reason;

    /**
     * Get the state of the job in the queue.
     *
     * @return The state
     */
    public int getState() {
        return state;
    }

    /**
     * Set the state of the job in the queue.
     *
     * @param stateParam The state to set
     */
    public void setState(final int stateParam) {
        this.state = stateParam;
    }

    /**
     * Get the power state of the job.
     *
     * @return True if on, False if off
     */
    public Boolean getPower() {
        return power;
    }

    /**
     * Set the power state of the job.
     *
     * @param powerParam True for on, False for off
     */
    public void setPower(final Boolean powerParam) {
        this.power = powerParam;
    }

    /**
     * Get the number of seconds to keep alive.
     *
     * @return The number of seconds
     */
    public double getKeepAlive() {
        return keepAlive;
    }

    /**
     * Set the number of seconds to keep alive.
     *
     * @param keepAliveParam The number of seconds to set
     */
    public void setKeepAlive(final double keepAliveParam) {
        this.keepAlive = keepAliveParam;
    }

    /**
     * Get the reason for the job being destroyed.
     *
     * @return The reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Set the reason for the job being destroyed.
     *
     * @param reasonParam The reason to set.
     */
    public void setReason(final String reasonParam) {
        this.reason = reasonParam;
    }
}
