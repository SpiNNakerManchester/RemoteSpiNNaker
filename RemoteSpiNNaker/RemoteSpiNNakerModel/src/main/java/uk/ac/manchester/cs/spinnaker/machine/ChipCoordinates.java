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
