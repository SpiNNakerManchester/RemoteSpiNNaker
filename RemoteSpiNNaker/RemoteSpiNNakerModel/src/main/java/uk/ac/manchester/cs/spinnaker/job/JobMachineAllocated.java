package uk.ac.manchester.cs.spinnaker.job;

/**
 * An allocated machine
 */
public class JobMachineAllocated {

    /**
     * True if the
     */
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
