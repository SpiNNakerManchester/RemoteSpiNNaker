package uk.ac.manchester.cs.spinnaker.status;

/**
 * Status Monitor that does nothing.
 *
 */
public class NullStatusMonitorManagerImpl implements StatusMonitorManager {

    @Override
    public void updateStatus(final int runningJobs, final int nBoardsInUse) {

        // Do Nothing
    }
}
