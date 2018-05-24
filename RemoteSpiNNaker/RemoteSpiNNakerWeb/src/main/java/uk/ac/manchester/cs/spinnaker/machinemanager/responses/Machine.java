package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import java.util.List;

/**
 * Describes a machine by its name, tags, width and height.
 */
public class Machine {
    private String name;
    private List<String> tags;
    private int width;
    private int height;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

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
}
