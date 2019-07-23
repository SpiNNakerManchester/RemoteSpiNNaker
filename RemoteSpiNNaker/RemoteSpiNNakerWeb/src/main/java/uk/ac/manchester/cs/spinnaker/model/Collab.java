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
 * POJO describing an HBP Collaboratory.
 */
public class Collab {

    /**
     * The content field.
     */
    private String content;

    /**
     * The collab ID.
     */
    private int id;

    /**
     * Get the content field.
     *
     * @return The content
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the content field.
     *
     * @param contentParam The value to set.
     */
    public void setContent(final String contentParam) {
        this.content = contentParam;
    }

    /**
     * Get the ID of the collab.
     *
     * @return The collab ID
     */
    public int getId() {
        return id;
    }

    /**
     * Set the ID of the collab.
     *
     * @param idParam The collab ID
     */
    public void setId(final int idParam) {
        this.id = idParam;
    }
}
