package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes a connection by its chip and hostname.
 */
@JsonPropertyOrder({"chip", "hostname"})
@JsonFormat(shape = ARRAY)
public class Connection {

    /**
     * The chip connected to.
     */
    private Chip chip;

    /**
     * The host name connected to.
     */
    private String hostname;

    /**
     * Get the chip connected to.
     *
     * @return The chip
     */
    public Chip getChip() {
        return chip;
    }

    /**
     * Set the chip connected to.
     *
     * @param chipParam The chip to set
     */
    public void setChip(final Chip chipParam) {
        this.chip = chipParam;
    }

    /**
     * Get the host name connected to.
     *
     * @return The host name
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Set the host name connected to.
     *
     * @param hostnameParam The host name to set
     */
    public void setHostname(final String hostnameParam) {
        this.hostname = hostnameParam;
    }
}
