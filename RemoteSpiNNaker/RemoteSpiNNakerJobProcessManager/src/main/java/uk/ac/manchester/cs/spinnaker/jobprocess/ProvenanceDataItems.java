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
	@XmlAttribute
	private String name;
	@XmlElement(name = "provenance_data_items")
	private final List<ProvenanceDataItems> provenanceDataItems
			= new ArrayList<>();
	@XmlElement(name = "provenance_data_item")
	private final List<ProvenanceDataItem> provenanceDataItem
			= new ArrayList<>();

	public String getName() {
		return name;
	}

	public List<ProvenanceDataItems> getProvenanceDataItems() {
		return provenanceDataItems;
	}

	public List<ProvenanceDataItem> getProvenanceDataItem() {
		return provenanceDataItem;
	}
}
