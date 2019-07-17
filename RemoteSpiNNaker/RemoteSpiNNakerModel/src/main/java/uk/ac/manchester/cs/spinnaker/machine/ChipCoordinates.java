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
package uk.ac.manchester.cs.spinnaker.machine;

/**
 * Represents a set of coordinates of a chip within a machine.
 */
public class ChipCoordinates {

    /**
     * The cabinet containing the frame containing the board containing the
     * chip.
     */
    private int cabinet;

    /**
     * The frame containing the board containing the chip.
     */
    private int frame;

    /**
     * The board containing the chip.
     */
    private int board;

    /**
     * Create a new set of ChipCoordinates.
     *
     * @param cabinetParam
     *            The cabinet containing the frame containing the board
     *            containing the chip.
     * @param frameParam
     *            The frame containing the board containing the chip.
     * @param boardParam
     *            The board containing the chip.
     */
    public ChipCoordinates(final int cabinetParam, final int frameParam,
            final int boardParam) {
        this.cabinet = cabinetParam;
        this.frame = frameParam;
        this.board = boardParam;
    }

    /**
     * Get the cabinet containing the frame containing the board containing the
     * chip.
     *
     * @return The cabinet number
     */
    public int getCabinet() {
        return cabinet;
    }

    /**
     * Get the frame containing the board containing the chip.
     *
     * @return The frame number
     */
    public int getFrame() {
        return frame;
    }

    /**
     * Get the board containing the chip.
     *
     * @return The board number
     */
    public int getBoard() {
        return board;
    }
}
