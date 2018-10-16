package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.util.List;

/**
 * A single item of provenance data.
 */
public class ProvenanceItem {

    /**
     * The path of the item.
     */
    private final List<String> path;

    /**
     * The value if the item.
     */
    private final String value;

    /**
     * Create a provenance item.
     *
     * @param pathParam
     *            The location of the item in the provenance tree.
     * @param valueParam
     *            The content of the value.
     */
    public ProvenanceItem(
            final List<String> pathParam, final String valueParam) {
        this.path = pathParam;
        this.value = valueParam;
    }

    /**
     * Get the path to the item.
     *
     * @return The path
     */
    public List<String> getPath() {
        return path;
    }

    /**
     * Get the value of the item.
     *
     * @return The value
     */
    public String getValue() {
        return value;
    }
}
