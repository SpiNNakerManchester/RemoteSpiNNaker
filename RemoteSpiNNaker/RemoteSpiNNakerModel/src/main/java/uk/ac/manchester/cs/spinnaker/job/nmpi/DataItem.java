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
package uk.ac.manchester.cs.spinnaker.job.nmpi;

/**
 * A reference to some data to be moved into or out of a {@link Job}.
 */
public class DataItem {

    /**
     * The item URL.
     */
    private String url;

    /**
     * Creates an empty item of data.
     */
    public DataItem() {
        // Does Nothing
    }

    /**
     * Make an instance that wraps a URL. The meaning of the URL depends on the
     * usage of the data item.
     *
     * @param urlParam
     *            The URL to wrap.
     */
    public DataItem(final String urlParam) {
        this.url = urlParam;
    }

    /**
     * Get the URL of the item of data.
     *
     * @return The URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the URL of the item of data.
     *
     * @param urlParam The URL
     */
    public void setUrl(final String urlParam) {
        this.url = urlParam;
    }
}
