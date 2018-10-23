package uk.ac.manchester.cs.spinnaker.model;

/**
 * POJO describing an HBP Collaboratory.
 */
public class Collab {

    /**
     * The content field.
     */
    private String content;

    /**
     * The collab ID.
     */
    private int id;

    /**
     * Get the content field.
     *
     * @return The content
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the content field.
     *
     * @param contentParam The value to set.
     */
    public void setContent(final String contentParam) {
        this.content = contentParam;
    }

    /**
     * Get the ID of the collab.
     *
     * @return The collab ID
     */
    public int getId() {
        return id;
    }

    /**
     * Set the ID of the collab.
     *
     * @param idParam The collab ID
     */
    public void setId(final int idParam) {
        this.id = idParam;
    }
}
