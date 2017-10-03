package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.util.List;

public class ProvenanceItem {
	private List<String> path;
	private String value;

	public ProvenanceItem(List<String> path, String value) {
		this.path = path;
		this.value = value;
	}

	public List<String> getPath() {
		return path;
	}

	public String getValue() {
		return value;
	}
}
