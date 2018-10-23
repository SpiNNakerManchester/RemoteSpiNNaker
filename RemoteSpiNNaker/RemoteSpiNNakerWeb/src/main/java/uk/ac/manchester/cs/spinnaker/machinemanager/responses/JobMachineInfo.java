package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import java.util.List;

/**
 * A description of a machine associated with a job, in terms of width, height,
 * connections and its name.
 */
public class JobMachineInfo {

    /**
     * The width of the machine.
     */
    private int width;

    /**
     * The height of the machine.
     */
    private int height;

    /**
     * The connections to the machine.
     */
    private List<Connection> connections;

    /**
     * The name of the machine.
     */
    private String machineName;

    /**
     * Get the width of the machine.
     *
     * @return The width in chips
     */
    public int getWidth() {
        return width;
    }

    /**
     * Set the width of the machine.
     *
     * @param widthParam The width in chips
     */
    public void setWidth(final int widthParam) {
        this.width = widthParam;
    }

    /**
     * Get the height of the machine.
     *
     * @return The height in chips
     */
    public int getHeight() {
        return height;
    }

    /**
     * Set the height of the machine.
     *
     * @param heightParam The height in chips
     */
    public void setHeight(final int heightParam) {
        this.height = heightParam;
    }

    /**
     * Get the connections to the machine.
     *
     * @return The connections
     */
    public List<Connection> getConnections() {
        return connections;
    }

    /**
     * Set the connection to the machine.
     *
     * @param connectionsParam The connections to set
     */
    public void setConnections(final List<Connection> connectionsParam) {
        this.connections = connectionsParam;
    }

    /**
     * Get the name of the machine.
     *
     * @return The name
     */
    public String getMachineName() {
        return machineName;
    }

    /**
     * Set the name of the machine.
     *
     * @param machineNameParam The name to set
     */
    public void setMachineName(final String machineNameParam) {
        this.machineName = machineNameParam;
    }
}
