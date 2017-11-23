package uk.ac.manchester.cs.spinnaker.machine;

import static java.lang.Integer.parseInt;

import java.io.Serializable;

/**
 * Represents a SpiNNaker machine on which jobs can be executed.
 */
public class SpinnakerMachine
        implements Serializable, Comparable<SpinnakerMachine> {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -2247744763327978524L;

    /**
     * The number of parts that make up a machine description as a string.
     */
    private static final int N_PARTS = 6;

    /**
     * Part of the string that is the name of the machine.
     */
    private static final int MACHINE_NAME_PART = 0;

    /**
     * Part of the string that is the version of the machine.
     */
    private static final int VERSION_PART = 1;

    /**
     * Part of the string that is the width of the machine.
     */
    private static final int WIDTH_PART = 2;

    /**
     * Part of the string that is the height of the machine.
     */
    private static final int HEIGHT_PART = 3;

    /**
     * Part of the string that is the number of boards in the machine.
     */
    private static final int N_BOARDS_PART = 4;

    /**
     * Part of the string that is the BMP details of the machine.
     */
    private static final int BMP_DETAILS_PART = 5;

    /**
     * The name of the machine.
     */
    private String machineName = null;

    /**
     * The version of the machine.
     */
    private String version = null;

    /**
     * The width of the machine.
     */
    private int width = 0;

    /**
     * The height of the machine.
     */
    private int height = 0;

    /**
     * The number of boards in the machine.
     */
    private int nBoards = 0;

    /**
     * The BMP details of the machine.
     */
    private String bmpDetails = null;

    /**
     * Creates an empty machine.
     */
    public SpinnakerMachine() {
        // Does Nothing
    }

    /**
     * Creates a new Spinnaker Machine by parsing the name of a machine.
     *
     * @param value The name of the machine to parse.
     * @return the machine parsed.
     */
    public static SpinnakerMachine parse(final String value) {
        if (!value.startsWith("(") || !value.endsWith(")")) {
            throw new IllegalArgumentException("Cannot convert string \""
                    + value + "\" - missing start and end brackets");
        }

        final String[] parts =
                value.substring(1, value.length() - 1).split(":");
        if (parts.length != N_PARTS) {
            throw new IllegalArgumentException(
                    "Wrong number of :-separated arguments - " + parts.length
                            + " found but 6 required");
        }

        return new SpinnakerMachine(
                parts[MACHINE_NAME_PART].trim(), parts[VERSION_PART].trim(),
                parseInt(parts[WIDTH_PART].trim()),
                parseInt(parts[HEIGHT_PART].trim()),
                parseInt(parts[N_BOARDS_PART].trim()),
                parts[BMP_DETAILS_PART].trim());
    }

    /**
     * Get a string version of the machine.
     */
    @Override
    public String toString() {
        String output = null;

        for (Object potential : new Object[]{
                machineName, version, bmpDetails, width, height, bmpDetails}) {
            if (potential != null) {
                if (output == null) {
                    output = potential.toString();
                } else {
                    output += ":" + potential.toString();
                }
            }
        }
        return output;
    }

    /**
     * Create a new SpiNNaker Machine.
     *
     * @param machineNameParam The name of the machine
     * @param versionParam The version of the machine
     * @param widthParam The width of the machine
     * @param heightParam The height of the machine
     * @param nBoardsParam The number of boards in the machine
     * @param bmpDetailsParam The details of the machine
     */
    public SpinnakerMachine(
            final String machineNameParam, final String versionParam,
            final int widthParam, final int heightParam, final int nBoardsParam,
            final String bmpDetailsParam) {
        this.machineName = machineNameParam;
        this.version = versionParam;
        this.width = widthParam;
        this.height = heightParam;
        this.nBoards = nBoardsParam;
        this.bmpDetails = bmpDetailsParam;
    }

    /**
     * Gets the name of the machine.
     *
     * @return The name of the machine
     */
    public String getMachineName() {
        return machineName;
    }

    /**
     * Sets the name of the machine.
     *
     * @param machineNameParam
     *            The name of the machine
     */
    public void setMachineName(final String machineNameParam) {
        this.machineName = machineNameParam;
    }

    /**
     * Gets the version of the machine.
     *
     * @return The version of the machine
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the machine.
     *
     * @param versionParam
     *            The version of the machine
     */
    public void setVersion(final String versionParam) {
        this.version = versionParam;
    }

    /**
     * Gets the width of the machine.
     *
     * @return The width of the machine
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of the machine.
     *
     * @param widthParam
     *            The width of the machine
     */
    public void setWidth(final int widthParam) {
        this.width = widthParam;
    }

    /**
     * Gets the height of the machine.
     *
     * @return The height of the machine
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of the machine.
     *
     * @param heightParam
     *            The height of the machine
     */
    public void setHeight(final int heightParam) {
        this.height = heightParam;
    }

    /** @return width &times; height */
    public int getArea() {
        return width * height;
    }

    /**
     * Gets the number of boards in the machine.
     *
     * @return The number of boards in the machine
     */
    public int getnBoards() {
        return nBoards;
    }

    /**
     * Sets the number of boards in the machine.
     *
     * @param nBoardsParam
     *            The number of boards in the machine
     */
    public void setnBoards(final int nBoardsParam) {
        this.nBoards = nBoardsParam;
    }

    /**
     * Gets the BMP details of the machine.
     *
     * @return The BMP details of the machine
     */
    public String getBmpDetails() {
        return bmpDetails;
    }

    /**
     * Sets the BMP details of the machine.
     *
     * @param bmpDetailsParam
     *            The BMP details of the machine
     */
    public void setBmpDetails(final String bmpDetailsParam) {
        this.bmpDetails = bmpDetailsParam;
    }

    /**
     * Check for equality with another machine.
     */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof SpinnakerMachine) {
            // TODO Is this the right way to determine equality?
            final SpinnakerMachine m = (SpinnakerMachine) o;
            if (machineName == null) {
                if (m.machineName != null) {
                    return false;
                }
            } else if (m.machineName == null) {
                return false;
            } else {
                if (!machineName.equals(m.machineName)) {
                    return false;
                }
            }
            if (version == null) {
                if (m.version != null) {
                    return false;
                }
            } else if (m.version == null) {
                return false;
            } else {
                if (!version.equals(m.version)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Compare to another machine; order by name then by version.
     */
    @Override
    public int compareTo(final SpinnakerMachine m) {
        int cmp = 0;
        if (machineName == null) {
            if (m.machineName == null) {
                cmp = 0;
            } else {
                cmp = -1;
            }
        } else if (m.machineName == null) {
            cmp = 1;
        } else {
            cmp = machineName.compareTo(m.machineName);
        }
        if (cmp == 0) {
            // TODO Is this the right way to compare versions? It works...
            if (version == null) {
                if (m.version == null) {
                    cmp = 0;
                } else {
                    cmp = -1;
                }
            } else if (m.version == null) {
                cmp = 1;
            } else {
                cmp = version.compareTo(m.version);
            }
        }
        return cmp;
    }

    /**
     * Generate a hash code based on name and version.
     */
    @Override
    public int hashCode() {
        // TODO Should be consistent with equality tests
        int hc = 0;
        if (machineName != null) {
            hc ^= machineName.hashCode();
        }
        if (version != null) {
            hc ^= version.hashCode();
        }
        return hc;
    }
}
