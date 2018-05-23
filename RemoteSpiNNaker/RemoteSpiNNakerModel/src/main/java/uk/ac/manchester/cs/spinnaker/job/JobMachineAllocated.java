package uk.ac.manchester.cs.spinnaker.job;

/**
 * Indicates whether a machine was allocated for a job.
 */
public class JobMachineAllocated {
    private boolean allocated = false;

    public JobMachineAllocated() {
        // Does Nothing
    }

    public JobMachineAllocated(final boolean allocated) {
        this.allocated = allocated;
    }

    public boolean isAllocated() {
        return this.allocated;
    }

    public void setAllocated(final boolean allocated) {
        this.allocated = allocated;
    }
}
