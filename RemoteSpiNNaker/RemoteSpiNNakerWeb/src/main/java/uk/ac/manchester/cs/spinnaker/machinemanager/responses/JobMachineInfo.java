package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import java.util.List;

/**
 * A description of a machine associated with a job, in terms of width, height,
 * connections and its name.
 */
public class JobMachineInfo {
    private int width;
    private int height;
    private List<Connection> connections;
    private String machineName;

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void setConnections(final List<Connection> connections) {
        this.connections = connections;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(final String machineName) {
        this.machineName = machineName;
    }
}
