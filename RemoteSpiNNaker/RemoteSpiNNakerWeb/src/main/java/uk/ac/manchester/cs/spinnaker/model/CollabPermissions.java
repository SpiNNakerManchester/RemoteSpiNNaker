package uk.ac.manchester.cs.spinnaker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO describing the permissions to a collabratory object.
 */
public class CollabPermissions {
    private boolean delete;
    private boolean update;

    @JsonProperty("DELETE")
    public boolean isDelete() {
        return delete;
    }

    public void setDelete(final boolean delete) {
        this.delete = delete;
    }

    @JsonProperty("UPDATE")
    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(final boolean update) {
        this.update = update;
    }

}
