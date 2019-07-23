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
package uk.ac.manchester.cs.spinnaker.remote.web;

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.cs.spinnaker.rest.utils.SpringRestClientUtils.createOIDCClient;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.cs.spinnaker.model.CollabPermissions;
import uk.ac.manchester.cs.spinnaker.rest.CollabRestService;

/**
 * The client for the HBP Collaboratory security service.
 */
public class CollabSecurityService {
    /**
     * Logging.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The URL of the collab web service.
     */
    @Value("${collab.service.uri}")
    private URL collabServiceUrl;

    /**
     * Create an instance of the service.
     * @return The instance created
     */
    private CollabRestService getServiceInstance() {
        // Do not factor out; depends on thread context
        return createOIDCClient(collabServiceUrl, CollabRestService.class);
    }

    /**
     * Get the permissions within the collab of the given id.
     * @param id The id to get the permissions of
     * @return The permissions granted or not
     */
    private CollabPermissions getPermissions(final int id) {
        return requireNonNull(getServiceInstance().getCollabPermissions(id));
    }

    /**
     * Test if the user has read permissions to the given ID.
     *
     * @param id
     *            Identifier to test against.
     * @return true if reading is permitted.
     */
    public boolean canRead(final int id) {
        try {
            getPermissions(id);
            return true;
        } catch (final Exception e) {
            logger.debug("Error getting collab permissions, "
                    + "assumed access denied", e);
            return false;
        }
    }

    /**
     * Test if the user has write permissions to the given ID.
     *
     * @param id
     *            Identifier to test against.
     * @return true if writing is permitted.
     */
    public boolean canUpdate(final int id) {
        try {
            return getPermissions(id).isUpdate();
        } catch (final Exception e) {
            logger.debug("Error getting collab permissions, "
                    + "assumed access denied", e);
            return false;
        }
    }

    /**
     * Test if the user has delete permissions to the given ID.
     *
     * @param id
     *            Identifier to test against.
     * @return true if deleting is permitted.
     */
    public boolean canDelete(final int id) {
        try {
            return getPermissions(id).isDelete();
        } catch (final Exception e) {
            logger.debug("Error getting collab permissions, "
                    + "assumed access denied", e);
            return false;
        }
    }
}
