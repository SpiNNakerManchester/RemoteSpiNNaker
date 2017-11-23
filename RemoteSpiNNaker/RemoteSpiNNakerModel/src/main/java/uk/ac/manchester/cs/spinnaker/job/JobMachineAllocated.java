package uk.ac.manchester.cs.spinnaker.job;

/**
 * An allocated machine.
 */
public class JobMachineAllocated {

    /**
     * True if the machine has been allocated.
     */
    private boolean allocated = false;

    /**
     * Empty for serialisation.
     */
    public JobMachineAllocated() {
        // Does Nothing
    }

    /**
     * Creates a allocated flag.
     *
     * @param allocatedParam The flag
     */
    public JobMachineAllocated(final boolean allocatedParam) {
        this.allocated = allocatedParam;
    }

    /**
     * Determine if the job is allocated.
     *
     * @return True if allocated
     */
    public boolean isAllocated() {
        return this.allocated;
    }

    /**
     * Set the job allocation status.
     *
     * @param allocatedParam The allocation status
     */
    public void setAllocated(final boolean allocatedParam) {
        this.allocated = allocatedParam;
    }
}
