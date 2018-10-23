package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import java.util.List;

/**
 * Describes a machine by its name, tags, width and height.
 */
public class Machine {

    /**
     * The name of the machine.
     */
    private String name;

    /**
     * The list of tags associated with the machine.
     */
    private List<String> tags;

    /**
     * The width of the machine.
     */
    private int width;

    /**
     * The height of the machine.
     */
    private int height;

    /**
     * Get the name of the machine.
     *
     * @return The name of the machine
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the machine.
     *
     * @param nameParam The name of the machine to set
     */
    public void setName(final String nameParam) {
        this.name = nameParam;
    }

    /**
     * Get the tags associated with the machine.
     *
     * @return The tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Set the tags associated with the machine.
     *
     * @param tagsParam The tags to set
     */
    public void setTags(final List<String> tagsParam) {
        this.tags = tagsParam;
    }

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
}
