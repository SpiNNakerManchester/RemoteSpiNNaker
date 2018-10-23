package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A response that is the result of listing the machines.
 */
@JsonFormat(shape = ARRAY)
public class ListMachinesResponse {

    /**
     * The list of machines.
     */
    private List<Machine> machines;

    /**
     * Get the list of machines.
     *
     * @return The list of machines
     */
    public List<Machine> getMachines() {
        return machines;
    }

    /**
     * Set the list of machines.
     *
     * @param machinesParam The list of machines to set
     */
    public void setMachines(final List<Machine> machinesParam) {
        this.machines = machinesParam;
    }
}
