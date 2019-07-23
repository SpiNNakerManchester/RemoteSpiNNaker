/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
