package uk.ac.manchester.cs.spinnaker.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;


public class ProvenanceJSON {

    private interface Contents {

    }

    private class SubProperties implements Contents {
        private Map<String, ProvenanceJSON> subproperties = new HashMap<>();

        @JsonValue
        public Map<String, ProvenanceJSON> getSubproperties() {
            return subproperties;
        }
    }

    private class Value implements Contents {
        private String value;

        private Value(String value) {
            this.value = value;
        }

        public void append(String value) {
            this.value += ", " + value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    private Contents contents;

    @JsonValue
    public Contents getContents() {
        return contents;
    }

    public void addItem(List<String> path, String value) {
        addItem(path, value, 0);
    }

    private void addItem(List<String> path, String value, int startOffset) {

        // Adding a value
        if (path.size() == startOffset) {
            if (this.contents instanceof SubProperties) {
                throw new RuntimeException(
                    "Can't add value \"" + value + "\" when already have"
                    + "subproperties at " + path + ", " + startOffset);
            }
            if (this.contents == null) {
                this.contents = new Value(value);
            } else {
                ((Value) this.contents).append(value);
            }

        // Adding a subitem
        } else {
            if (this.contents instanceof Value) {
                throw new RuntimeException(
                    "Can't add sub properties when already have a value \""
                    + this.contents + "\" at" + path + ", " + startOffset);
            }
            if (this.contents == null) {
                this.contents = new SubProperties();
            }
            String id = path.get(startOffset);
            SubProperties subproperties = (SubProperties) this.contents;
            if (!subproperties.subproperties.containsKey(id)) {
                subproperties.subproperties.put(id, new ProvenanceJSON());
            }
            subproperties.subproperties.get(id).addItem(
                path, value, startOffset + 1);
        }
    }
}
