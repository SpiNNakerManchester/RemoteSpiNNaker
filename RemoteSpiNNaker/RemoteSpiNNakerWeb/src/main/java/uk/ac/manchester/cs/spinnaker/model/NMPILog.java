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
package uk.ac.manchester.cs.spinnaker.model;

/**
 * A Neuromorphic Platform Interface log core.
 */
public class NMPILog {
    /**
     * The content of the log.
     */
    private StringBuilder buffer;

    /**
     * Gets the current log contents.
     *
     * @return The log contents, or {@code null} if the log is not yet
     *         initialised.
     */
    public String getContent() {
        if (buffer == null) {
            return null;
        }
        return buffer.toString();
    }

    /**
     * Set the content.
     *
     * @param content The content to set
     */
    public void setContent(final String content) {
        this.buffer = new StringBuilder(content);
    }

    /**
     * Append the string to the log.
     *
     * @param content
     *            The string to append.
     */
    public void appendContent(final String content) {
        if (this.buffer == null) {
            this.buffer = new StringBuilder(content);
        } else {
            this.buffer.append(content);
        }
    }
}
