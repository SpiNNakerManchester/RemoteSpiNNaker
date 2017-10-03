package uk.ac.manchester.cs.spinnaker.jobprocess;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class ProvenanceDataItem {

	@XmlAttribute
	private String name;

	@XmlValue
	private String value;

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}
}
