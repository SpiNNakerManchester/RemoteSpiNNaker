package uk.ac.manchester.cs.spinnaker.model;

/**
 * POJO describing an HBP Collabratory.
 */
public class Collab {
    private String content;
    private int id;

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }
}
