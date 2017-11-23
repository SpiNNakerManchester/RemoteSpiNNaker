package uk.ac.manchester.cs.spinnaker.jobprocess;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * An item of provenance data.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ProvenanceDataItem {

    /**
     * The name of the item.
     */
    @XmlAttribute
    private String name;

    /**
     * The value if the item.
     */
    @XmlValue
    private String value;

    /**
     * Get the name of the item.
     *
     * @return The name of the item
     */
    public String getName() {
        return name;
    }

    /**
     * Get the value of the item.
     *
     * @return The value of the item
     */
    public String getValue() {
        return value;
    }
}
