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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Describes a chip by its X,Y location.
 */
@JsonPropertyOrder({"x", "y"})
@JsonFormat(shape = ARRAY)
public class Chip {

    /**
     * The x-coordinate of the chip.
     */
    private int x;

    /**
     * The y-coordinate of the chip.
     */
    private int y;

    /**
     * Get the x-coordinate of the chip.
     *
     * @return The x-coordinate of the chip
     */
    public int getX() {
        return x;
    }

    /**
     * Set the x-coordinate of the chip.
     *
     * @param xParam The x-coordinate of the chip
     */
    public void setX(final int xParam) {
        this.x = xParam;
    }

    /**
     * Get the y-coordinate of the chip.
     *
     * @return The y-coordinate of the chip
     */
    public int getY() {
        return y;
    }

    /**
     * Set the y-coordinate of the chip.
     *
     * @param yParam The y-coordinate of the chip
     */
    public void setY(final int yParam) {
        this.y = yParam;
    }
}
