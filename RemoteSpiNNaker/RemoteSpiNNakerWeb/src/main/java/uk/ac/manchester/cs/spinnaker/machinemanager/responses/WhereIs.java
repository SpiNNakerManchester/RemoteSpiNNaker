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
 * The description of where some resource is on a SpiNNaker system.
 */
public class WhereIs {

    /**
     * The job-relative location of the chip.
     */
    private int[] jobChip;

    /**
     * The id of the job.
     */
    private int jobId;

    /**
     * The physical location of the chip.
     */
    private int[] chip;

    /**
     * The logical location of the job.
     */
    private int[] logical;

    /**
     * The machine if the job.
     */
    private String machine;

    /**
     * The board-relative location of the chip.
     */
    private int[] boardChip;

    /**
     * The physical location of the job.
     */
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
     * Sets the job chip.
     *
     * @param jobChipParam
     *            the job chip to set
     */
    public void setJobChip(final int[] jobChipParam) {
        this.jobChip = jobChipParam;
    }

    /**
     * Get the job id.
     *
     * @return the job id
     */
    public int getJobId() {
        return jobId;
    }

    /**
     * Sets the job id.
     *
     * @param jobIdParam
     *            the job id to set
     */
    public void setJobId(final int jobIdParam) {
        this.jobId = jobIdParam;
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
     * @param chipParam
     *            the chip to set
     */
    public void setChip(final int[] chipParam) {
        this.chip = chipParam;
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
     * @param logicalParam
     *            the logical to set
     */
    public void setLogical(final int[] logicalParam) {
        this.logical = logicalParam;
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
     * @param machineParam
     *            the machine to set
     */
    public void setMachine(final String machineParam) {
        this.machine = machineParam;
    }

    /**
     * Get the board chip.
     *
     * @return the board chip
     */
    public int[] getBoardChip() {
        return boardChip;
    }

    /**
     * Sets the board chip.
     *
     * @param boardChipParam
     *            the board chip to set
     */
    public void setBoardChip(final int[] boardChipParam) {
        this.boardChip = boardChipParam;
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
     * @param physicalParam
     *            the physical to set
     */
    public void setPhysical(final int[] physicalParam) {
        this.physical = physicalParam;
    }
}
