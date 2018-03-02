package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

public class WhereIs {
    private int[] jobChip;
    private int jobId;
    private int[] chip;
    private int[] logical;
    private String machine;
    private int[] boardChip;
    private int[] physical;
    /**
     * Get the job_chip.
     *
     * @return the job_chip
     */
    public int[] getJobChip() {
        return jobChip;
    }
    /**
     * Sets the job_chip.
     *
     * @param job_chip the job_chip to set
     */
    public void setJobChip(final int[] jobChip) {
        this.jobChip = jobChip;
    }
    /**
     * Get the job_id.
     *
     * @return the job_id
     */
    public int getJobId() {
        return jobId;
    }
    /**
     * Sets the job_id.
     *
     * @param job_id the job_id to set
     */
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
    /**
     * Get the chip.
     *
     * @return the chip
     */
    public int[] getChip() {
        return chip;
    }
    /**
     * Sets the chip.
     *
     * @param chip the chip to set
     */
    public void setChip(int[] chip) {
        this.chip = chip;
    }
    /**
     * Get the logical.
     *
     * @return the logical
     */
    public int[] getLogical() {
        return logical;
    }
    /**
     * Sets the logical.
     *
     * @param logical the logical to set
     */
    public void setLogical(int[] logical) {
        this.logical = logical;
    }
    /**
     * Get the machine.
     *
     * @return the machine
     */
    public String getMachine() {
        return machine;
    }
    /**
     * Sets the machine.
     *
     * @param machine the machine to set
     */
    public void setMachine(String machine) {
        this.machine = machine;
    }
    /**
     * Get the board_chip.
     *
     * @return the board_chip
     */
    public int[] getBoardChip() {
        return boardChip;
    }
    /**
     * Sets the board_chip.
     *
     * @param boardChip the board_chip to set
     */
    public void setBoardChip(int[] boardChip) {
        this.boardChip = boardChip;
    }
    /**
     * Get the physical.
     *
     * @return the physical
     */
    public int[] getPhysical() {
        return physical;
    }
    /**
     * Sets the physical.
     *
     * @param physical the physical to set
     */
    public void setPhysical(int[] physical) {
        this.physical = physical;
    }

}
