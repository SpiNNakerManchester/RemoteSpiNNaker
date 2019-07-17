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
package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.util.List;

/**
 * A single item of provenance data.
 */
public class ProvenanceItem {

    /**
     * The path of the item.
     */
    private final List<String> path;

    /**
     * The value if the item.
     */
    private final String value;

    /**
     * Create a provenance item.
     *
     * @param pathParam
     *            The location of the item in the provenance tree.
     * @param valueParam
     *            The content of the value.
     */
    public ProvenanceItem(
            final List<String> pathParam, final String valueParam) {
        this.path = pathParam;
        this.value = valueParam;
    }

    /**
     * Get the path to the item.
     *
     * @return The path
     */
    public List<String> getPath() {
        return path;
    }

    /**
     * Get the value of the item.
     *
     * @return The value
     */
    public String getValue() {
        return value;
    }
}
