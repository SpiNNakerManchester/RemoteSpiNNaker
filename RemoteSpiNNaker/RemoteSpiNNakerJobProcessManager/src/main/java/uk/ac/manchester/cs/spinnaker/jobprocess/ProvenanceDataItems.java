package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A hierarchy of provenance data items.
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
