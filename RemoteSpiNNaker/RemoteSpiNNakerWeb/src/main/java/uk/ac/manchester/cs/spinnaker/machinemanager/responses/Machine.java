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
