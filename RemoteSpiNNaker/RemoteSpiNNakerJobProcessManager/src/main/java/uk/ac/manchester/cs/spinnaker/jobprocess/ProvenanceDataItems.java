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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The POJO representation of a collection of data items.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "provenance_data_items")
public class ProvenanceDataItems {

    /**
     * The name of this level of the hierarchy.
     */
    @XmlAttribute
    private String name;

    /**
     * The sub hierarchies.
     */
    @XmlElement(name = "provenance_data_items")
    private final List<ProvenanceDataItems> provenanceDataItems =
            new ArrayList<>();

    /**
     * Items at this level of the hierarchy.
     */
    @XmlElement(name = "provenance_data_item")
    private final List<ProvenanceDataItem> provenanceDataItem =
            new ArrayList<>();

    /**
     * Get the name of this level of the hierarchy.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Levels below this level of the hierarchy.
     *
     * @return The child levels
     */
    public List<ProvenanceDataItems> getProvenanceDataItems() {
        return provenanceDataItems;
    }

    /**
     * Items at this level of the hierarchy.
     *
     * @return The items at this level
     */
    public List<ProvenanceDataItem> getProvenanceDataItem() {
        return provenanceDataItem;
    }
}
