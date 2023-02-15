/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
