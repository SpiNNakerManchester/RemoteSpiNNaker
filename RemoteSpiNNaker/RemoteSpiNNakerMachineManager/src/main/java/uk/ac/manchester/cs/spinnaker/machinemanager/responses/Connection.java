package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"chip", "hostname"})
@JsonFormat(shape=JsonFormat.Shape.ARRAY)
public class Connection {
    private Chip chip;
    private String hostname;

    public Chip getChip() {
        return chip;
    }

    public void setChip(Chip chip) {
        this.chip = chip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
