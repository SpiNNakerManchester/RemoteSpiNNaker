package uk.ac.manchester.cs.spinnaker.status;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
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
    private final Logger logger = getLogger(getClass());

    /**
     * Initialise the service.
     */
    @PostConstruct
    private void init() {
        final ResteasyClient client = new ResteasyClientBuilder().build();
        statusCake = client.target(SERVICE_URL).proxy(StatusCake.class);
    }

    @Override
    public void updateStatus(final int runningJobs, final int nBoardsInUse) {
        logger.debug("Updating to Status Cake");
        try {
            statusCake.pushUpdate(primaryKey, testID, nBoardsInUse);
        } catch (Throwable e) {
            logger.error("Error updating to Status Cake", e);
        }
    }
}
