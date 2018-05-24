package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.util.List;

/**
 * A single item of provenance data.
 */
public class ProvenanceItem {

    private List<String> path;

    private String value;

    /**
     * Create a provenance item.
     *
     * @param path
     *            The location of the item in the provenance tree.
     * @param value
     *            The content of the value.
     */
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
