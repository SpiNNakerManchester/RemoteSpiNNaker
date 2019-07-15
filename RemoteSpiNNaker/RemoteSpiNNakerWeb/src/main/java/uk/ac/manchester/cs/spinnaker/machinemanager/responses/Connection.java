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
 * Describes a connection by its chip and hostname.
 */
@JsonPropertyOrder({"chip", "hostname"})
@JsonFormat(shape = ARRAY)
public class Connection {

    /**
     * The chip connected to.
     */
    private Chip chip;

    /**
     * The host name connected to.
     */
    private String hostname;

    /**
     * Get the chip connected to.
     *
     * @return The chip
     */
    public Chip getChip() {
        return chip;
    }

    /**
     * Set the chip connected to.
     *
     * @param chipParam The chip to set
     */
    public void setChip(final Chip chipParam) {
        this.chip = chipParam;
    }

    /**
     * Get the host name connected to.
     *
     * @return The host name
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Set the host name connected to.
     *
     * @param hostnameParam The host name to set
     */
    public void setHostname(final String hostnameParam) {
        this.hostname = hostnameParam;
    }
}
