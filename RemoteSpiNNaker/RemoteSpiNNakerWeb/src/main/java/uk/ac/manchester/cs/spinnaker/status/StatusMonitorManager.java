package uk.ac.manchester.cs.spinnaker.status;

/**
 * A service that handles status monitoring.
 */
public interface StatusMonitorManager {

    /**
     * Send a heart beat to the status monitoring service to indicate that we
     * are alive.
     *
     * @param runningJobs The number of running jobs.
     * @param nBoardsInUse The number of boards currently allocated.
     */
    void updateStatus(int runningJobs, int nBoardsInUse);
}
