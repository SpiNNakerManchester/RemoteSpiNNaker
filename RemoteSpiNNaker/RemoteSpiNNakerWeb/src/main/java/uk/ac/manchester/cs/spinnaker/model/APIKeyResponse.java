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
 * POJO holding the response for a query for an API key.
 */
public class APIKeyResponse {

    /**
     * The API Key.
     */
    private String key;

    /**
     * Get the API Key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the API key.
     *
     * @param keyParam The key to set
     */
    public void setKey(final String keyParam) {
        this.key = keyParam;
    }
}
