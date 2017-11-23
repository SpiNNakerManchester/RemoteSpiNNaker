package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.util.List;

/**
 * An item of provenance in a hierarchy with a path.
 *
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
     * Create a new item.
     *
     * @param pathParam The path to the item
     * @param valueParam The value of the item
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
