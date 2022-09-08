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
package uk.ac.manchester.cs.spinnaker.status;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.clientBuilder;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.cs.spinnaker.rest.StatusCake;

/**
 * Status monitor manager that uses StatusCake service.
 *
 */
public class StatusCakeStatusMonitorManagerImpl
        implements StatusMonitorManager {

    /**
     * The URL of the service.
     */
    private static final String SERVICE_URL = "https://push.statuscake.com/";

    /**
     * The Timeout of socket operations in seconds.
     */
    private static final long TIMEOUT = 60;

    /**
     * The REST API to call.
     */
    private StatusCake statusCake;

    /**
     * The Primary Key to use for updates.
     */
    @Value("${statusCake.primaryKey}")
    private String primaryKey;

    /**
     * The Test ID to use for updates.
     */
    @Value("${statusCake.testID}")
    private String testID;

    /**
     * Logging.
     */
    private static final Logger logger =
            getLogger(StatusCakeStatusMonitorManagerImpl.class);

    /**
     * Initialise the service.
     */
    @PostConstruct
    private void init() {
        // TODO use RestClientUtils.createClient()
        final var client = clientBuilder().
                connectTimeout(TIMEOUT, SECONDS).
                readTimeout(TIMEOUT, SECONDS).build();
        statusCake = client.target(SERVICE_URL).proxy(StatusCake.class);
    }

    @Override
    public void updateStatus(final int runningJobs, final int nBoardsInUse) {
        logger.debug("Updating to Status Cake - "
                + "runningJobs = {}, nBoardsInUse = {}",
                runningJobs, nBoardsInUse);
        try {
            statusCake.pushUpdate(primaryKey, testID, nBoardsInUse);
        } catch (Throwable e) {
            logger.error("Error updating to Status Cake", e);
        }
    }
}
