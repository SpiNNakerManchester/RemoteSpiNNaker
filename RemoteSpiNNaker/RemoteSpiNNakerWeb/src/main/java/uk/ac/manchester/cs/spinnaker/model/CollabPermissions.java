package uk.ac.manchester.cs.spinnaker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO describing the permissions to a collaboratory object.
 */
public class CollabPermissions {

    /**
     * True if delete permission.
     */
    private boolean delete;

    /**
     * True if update permission.
     */
    private boolean update;

    /**
     * Determine if delete permission is given.
     *
     * @return Whether permitted to delete
     */
    @JsonProperty("DELETE")
    public boolean isDelete() {
        return delete;
    }

    /**
     * Set delete permission.
     *
     * @param deleteParam Whether to give permission
     */
    public void setDelete(final boolean deleteParam) {
        this.delete = deleteParam;
    }

    /**
     * Determine if update permission is given.
     *
     * @return Whether permitted to update
     */
    @JsonProperty("UPDATE")
    public boolean isUpdate() {
        return update;
    }

    /**
     * Set update permission.
     *
     * @param updateParam Whether permission is given
     */
    public void setUpdate(final boolean updateParam) {
        this.update = updateParam;
    }

}
