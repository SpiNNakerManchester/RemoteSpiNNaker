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
    private StringBuilder content;

    /**
     * Gets the current log contents.
     *
     * @return The log contents, or <tt>null</tt> if the log is not yet
     *         initialised.
     */
    public String getContent() {
        if (content == null) {
            return null;
        }
        return content.toString();
    }

    /**
     * Set the content.
     *
     * @param contentParam The content to set
     */
    public void setContent(final String contentParam) {
        this.content = new StringBuilder(contentParam);
    }

    /**
     * Append the string to the log.
     *
     * @param contentParam
     *            The string to append.
     */
    public void appendContent(final String contentParam) {
        if (this.content == null) {
            this.content = new StringBuilder(contentParam);
        } else {
            this.content.append(contentParam);
        }
    }
}
